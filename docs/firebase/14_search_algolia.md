# 14: Product search with Algolia

**Decided:** product search uses **Algolia**. Firestore cannot search inside text ("air run" → "Air Runner 2"), cannot rank by relevance, and cannot forgive typos. Algolia does all three.

**Screens (Figma section 05b · Search):** the search bar on Home (04.1) and Categories (05.1) opens:
- **05b.1** empty query: recent searches, trending, popular categories, recently viewed.
- **05b.2** results: chips, "128 results for “runner”", 2-column grid.
- **05b.3** the sort sheet.
- **05b.4** no results: "Did you mean …", popular chips, "You might like".

Figma's note: *"Algolia: instant results as you type, typo tolerance, query suggestions."*

**Before you start:** docs 03 (products), 07 (rules), 09 (functions skeleton) and 13 (Android wiring).

---

## 1. How it works

```
admin tool writes products/{id}
        │
        ▼  (Firebase extension "Search Firestore with Algolia")
Algolia index "products"  ◄── settings: searchable fields, filters, ranking
        ▲
        │  searchProducts (callable, server holds the Algolia key)
Android SearchViewModel ── debounce 300 ms ── SearchRepository ── FirebaseFunctions
        │
        ▼
results show product cards; tapping one opens 06.1, which reads Firestore as usual
```

**Why a callable function instead of calling Algolia from the phone?**

| | Callable `searchProducts` (**we use this**) | Algolia client on the phone |
|---|---|---|
| Algolia key on the device | Never | A search-only key ships in the APK (or a secured key from a function) |
| Who enforces "signed in only" and "active, in this store" | The function, always | Filters sent by the app (can be edited) unless you mint secured keys |
| New dependencies on Android | None (`firebase-functions` is already planned in 01) | Algolia Kotlin client + a Ktor engine |
| Latency | +100–300 ms (function hop) | Fastest |

For v1 we pick the callable: one place for the rules, nothing secret on the phone, no new library. If search ever feels slow, move to the phone client with **secured API keys** minted by a function. The index and settings below stay the same.

**Single source of truth:** Algolia holds a **copy** of product fields for searching only. Prices, stock and details on the product page are always read from Firestore. This copy is listed as an allowed copy (see 02), and the extension is its only writer.

---

## 2. Algolia account and index

1. Create an Algolia account and an application in the **EU** region (closest to `europe-west1`).
2. Create an index named **`products`**.
3. In **Settings → API Keys**, you need:
   - the **Application ID**
   - the **Admin API key**. Only the extension uses it.
   - a new key with ACL **`search`** only, restricted to the `products` index. Only `searchProducts` uses it.

### Index settings (Algolia dashboard → Index → Configuration)

| Setting | Value | Why |
|---|---|---|
| Searchable attributes (in this order) | `name`, `brandName`, `subtitle.en`, `subtitle.fr`, `subtitle.ar` | Name matches rank highest. `name` and `brandName` are not translated (see 03), `subtitle` is |
| Attributes for faceting | `filterOnly(isActive)`, `filterOnly(inStockStoreIds)`, `categoryPath`, `brandId`, `badges` | Hidden filters + the chips on the results screen |
| Numeric attributes for filtering | `discountPercent`, `minPrice` | "On sale" chip = `discountPercent > 0` (05b.2) |
| Custom ranking | `desc(popularityScore)`, `desc(ratingAvg)` | Two equally relevant results: the popular one first |
| Replicas (virtual) | `products_price_asc` → `asc(minPrice)`, `products_price_desc` → `desc(minPrice)`, `products_newest` → `desc(publishedAt)` | Sort sheet 05b.3: Relevance / Price: low to high / Price: high to low / Newest first |
| Typo tolerance | on (default) | "air runer" still finds "Air Runner 2" |
| Ignore plurals | `en`, `fr` | "sneaker" = "sneakers" |
| Query languages | `en`, `fr`, `ar` | Arabic normalization |
| Attributes to retrieve | `name`, `brandName`, `images`, `minPrice`, `compareAtPrice`, `discountPercent`, `ratingAvg`, `ratingCount`, `badges`, `prepMinutes` | Only what a product card needs |

### Query Suggestions index (trending + "Did you mean")

In the Algolia dashboard, open **Query Suggestions** and create **`products_query_suggestions`** with source index `products`.
- Algolia builds it from the **search analytics** of `products`, i.e. what people actually typed and that returned results. While there is no traffic yet, add **facet-based** suggestions (`categoryPath`, `brandName`) and a few manual entries ("sneakers", "pulse max", "summer bags").
- Each record has a `query` (e.g. `"air runner"`) and a `popularity` score.
- We use it twice:
  - **Trending now (05b.1):** an empty-query search on the suggestions index returns the most popular queries. Shown as chips "Sneakers · Pulse Max · Summer bags · Watches · Skincare".
  - **Did you mean (05b.4):** when the products search returns **0 hits**, the function searches the suggestions index with the same text. Typo tolerance turns "air jordn" into a hit on the suggestion "air runner" ("jordn" fails, "air" matches; the best-ranked suggestion wins), and the top suggestion becomes `suggestion`.

**Be honest about what this is:** Algolia's normal search response doesn't contain a "corrected query" field. Typo tolerance already finds "air runer" → "Air Runner 2" **inside** the results. "Did you mean" is our own step: a second search on the suggestions index, made only when the first one finds nothing. If the suggestions index has no close match, the screen shows only "Popular right now" and "You might like".

---

## 3. Firebase extension: "Search Firestore with Algolia"

Install from the Firebase console (**Extensions → Search Firestore with Algolia**, publisher Algolia), or with the CLI:

```bash
firebase ext:install algolia/firestore-algolia-search --project <your-project-id>
```

Answer the install questions like this:

| Question | Answer |
|---|---|
| Location | `europe-west1` |
| Collection path | `products` |
| Indexable fields | `name,subtitle,brandId,brandName,categoryPath,images,badges,minPrice,compareAtPrice,discountPercent,ratingAvg,ratingCount,popularityScore,inStockStoreIds,prepMinutes,isActive,publishedAt` |
| Algolia index name | `products` |
| Algolia application id | your Application ID |
| Algolia API key | the **Admin** key (stored by the extension in Secret Manager) |
| Force data sync | No |

Only the fields listed are copied. `description` and `updatedAt` stay out of Algolia, which keeps records small.

**`publishedAt` must be a number in Algolia** for the `products_newest` replica to sort on it. After the first import, open one record in the Algolia dashboard:
- **If `publishedAt` shows as a number** (the extension converts Firestore Timestamps), you're done.
- **If it shows as an object or a string,** add `publishedAtMs` (number, written by the admin tool next to `publishedAt`) to `products` and to the indexable fields, list it in 02 § 4 as an allowed copy, and sort the replica on `desc(publishedAtMs)` instead.

**Existing products:** the extension syncs on every write. To push products that already exist, run the extension's **full index** import once (described in the extension's README, linked from its console page).

**Deleted products:** we soft-delete (`isActive = false`, see 02). The record stays in Algolia, and the `isActive:true` filter hides it.

---

## 4. Cloud Function `searchProducts`

`firebase/functions/src/search/searchProducts.ts`:

```typescript
import { algoliasearch } from "algoliasearch";
import { defineSecret, defineString } from "firebase-functions/params";
import { HttpsError, onCall } from "firebase-functions/v2/https";
import { requireAuth } from "../shared/errors";

const ALGOLIA_APP_ID = defineString("ALGOLIA_APP_ID");
const ALGOLIA_SEARCH_KEY = defineSecret("ALGOLIA_SEARCH_KEY");

type Sort = "relevance" | "price_asc" | "price_desc" | "newest";

interface SearchRequest {
  query: string;
  storeId: string;       // the cart's store (from the delivery zone, see 03)
  categoryId?: string;   // chip filter (Sneakers, Running, Kids)
  brandId?: string;
  onSale?: boolean;      // "On sale" chip
  sort?: Sort;
  page?: number;         // 0-based
}

const INDEX_BY_SORT: Record<Sort, string> = {
  relevance: "products",
  price_asc: "products_price_asc",
  price_desc: "products_price_desc",
  newest: "products_newest",
};
const SUGGESTIONS_INDEX = "products_query_suggestions";

/** Algolia filter values are quoted, so an id can't inject extra filter syntax. */
const quote = (v: string) => `"${v.replace(/"/g, "")}"`;

export const searchProducts = onCall<SearchRequest>(
  { enforceAppCheck: true, secrets: [ALGOLIA_SEARCH_KEY] },
  async (req) => {
    requireAuth(req.auth); // decided: no guest browsing

    const query = (req.data.query ?? "").trim().slice(0, 100);
    const page = Math.min(Math.max(req.data.page ?? 0, 0), 20);
    if (!req.data.storeId) throw new HttpsError("invalid-argument", "storeId is required");

    const filters = [
      "isActive:true",
      `inStockStoreIds:${quote(req.data.storeId)}`,
      req.data.categoryId ? `categoryPath:${quote(req.data.categoryId)}` : null,
      req.data.brandId ? `brandId:${quote(req.data.brandId)}` : null,
      req.data.onSale ? "discountPercent > 0" : null,
    ].filter(Boolean).join(" AND ");

    const client = algoliasearch(ALGOLIA_APP_ID.value(), ALGOLIA_SEARCH_KEY.value());
    const res = await client.searchSingleIndex({
      indexName: INDEX_BY_SORT[req.data.sort ?? "relevance"],
      searchParams: { query, filters, page, hitsPerPage: 20, facets: ["categoryPath", "brandId"] },
    });

    // 05b.4 "Did you mean …": only when nothing was found, ask the suggestions index.
    let suggestion: string | null = null;
    if ((res.nbHits ?? 0) === 0 && query.length >= 2) {
      const s = await client.searchSingleIndex({
        indexName: SUGGESTIONS_INDEX,
        searchParams: { query, hitsPerPage: 1 },
      });
      const top = (s.hits[0] as any)?.query as string | undefined;
      if (top && top.toLowerCase() !== query.toLowerCase()) suggestion = top;
    }

    return {
      suggestion,
      hits: res.hits.map((h: any) => ({
        productId: h.objectID, // the extension uses the Firestore document id as objectID
        name: h.name,
        brandName: h.brandName,
        imageUrl: (h.images ?? [])[0] ?? null,
        minPrice: h.minPrice,
        compareAtPrice: h.compareAtPrice ?? null,
        discountPercent: h.discountPercent ?? 0,
        ratingAvg: h.ratingAvg ?? 0,
        ratingCount: h.ratingCount ?? 0,
        badges: h.badges ?? [],
        prepMinutes: h.prepMinutes ?? 0,
      })),
      totalHits: res.nbHits ?? 0,
      page: res.page ?? page,
      pageCount: res.nbPages ?? 1,
      categoryCounts: res.facets?.categoryPath ?? {},
    };
  },
);

/** 05b.1 "Trending now": the most popular queries (empty query on the suggestions index). */
export const searchSuggestions = onCall<{ limit?: number }>(
  { enforceAppCheck: true, secrets: [ALGOLIA_SEARCH_KEY] },
  async (req) => {
    requireAuth(req.auth);
    const client = algoliasearch(ALGOLIA_APP_ID.value(), ALGOLIA_SEARCH_KEY.value());
    const res = await client.searchSingleIndex({
      indexName: SUGGESTIONS_INDEX,
      searchParams: { query: "", hitsPerPage: Math.min(req.data.limit ?? 5, 10) },
    });
    return { trending: res.hits.map((h: any) => h.query as string) };
  },
);
```

The search-only key must also be allowed on `products_query_suggestions` and on the replicas (restrict the key to `products*`).

Install and configure:

```bash
cd firebase/functions
npm install algoliasearch
firebase functions:secrets:set ALGOLIA_SEARCH_KEY   # paste the search-only key
# ALGOLIA_APP_ID goes in firebase/functions/.env:
#   ALGOLIA_APP_ID=YOUR_APP_ID
```

**Why each line:**
- `requireAuth`: rules can't protect Algolia, so the function enforces sign-in.
- `isActive:true` and `inStockStoreIds` are added on the server. The app can't ask for hidden or unavailable products.
- `quote()`: a crafted `categoryId` can't add `OR isActive:false`.
- `page ≤ 20` and `hitsPerPage = 20`: nobody can dump the whole catalog in one call.
- `secrets`: the key lives in Secret Manager, not in code or git.

**Algolia client version:** this uses the v5 JavaScript client (`algoliasearch` package, `searchSingleIndex`). If `npm install` gives you another major version, check the method name in Algolia's JS docs.

---

## 5. Android

### 5.1 Data layer

`features/search/data/dto/SearchResultDto.kt`:

```kotlin
package com.example.dukkanapp.features.search.data.dto

/** Shape returned by the searchProducts callable. Money in millimes (see 02). */
data class SearchHitDto(
    val productId: String,
    val name: String,
    val brandName: String,
    val imageUrl: String?,
    val minPrice: Long,
    val compareAtPrice: Long?,
    val discountPercent: Int,
    val ratingAvg: Double,
    val ratingCount: Long,
    val badges: List<String>,
    val prepMinutes: Int,
)

data class SearchPageDto(
    val hits: List<SearchHitDto>,
    val totalHits: Int,
    val page: Int,
    val pageCount: Int,
    val suggestion: String?, // "Did you mean air runner?" (only when totalHits == 0)
)
```

`features/search/data/datasource/SearchRemoteDataSource.kt`:

```kotlin
package com.example.dukkanapp.features.search.data.datasource

import com.example.dukkanapp.core.error.guard.guardException
import com.example.dukkanapp.features.search.data.dto.SearchHitDto
import com.example.dukkanapp.features.search.data.dto.SearchPageDto
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class SearchRemoteDataSource @Inject constructor(
    private val functions: FirebaseFunctions,
) {
    suspend fun search(
        query: String,
        storeId: String,
        categoryId: String?,
        onSale: Boolean,
        sort: String,
        page: Int,
    ): SearchPageDto = guardException {
        val request = buildMap<String, Any> {
            put("query", query)
            put("storeId", storeId)
            put("onSale", onSale)
            put("sort", sort)
            put("page", page)
            categoryId?.let { put("categoryId", it) }
        }
        val result = functions.getHttpsCallable("searchProducts").call(request).await()
        @Suppress("UNCHECKED_CAST")
        (result.getData() as Map<String, Any?>).toSearchPage()
    }

    /** 05b.1 "Trending now". */
    suspend fun trending(limit: Int = 5): List<String> = guardException {
        val result = functions.getHttpsCallable("searchSuggestions").call(mapOf("limit" to limit)).await()
        @Suppress("UNCHECKED_CAST")
        ((result.getData() as Map<String, Any?>)["trending"] as List<String>)
    }

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any?>.toSearchPage(): SearchPageDto = SearchPageDto(
        hits = (this["hits"] as List<Map<String, Any?>>).map { it.toHit() },
        totalHits = (this["totalHits"] as Number).toInt(),
        page = (this["page"] as Number).toInt(),
        pageCount = (this["pageCount"] as Number).toInt(),
        suggestion = this["suggestion"] as String?,
    )

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any?>.toHit(): SearchHitDto = SearchHitDto(
        productId = this["productId"] as String,
        name = this["name"] as String,
        brandName = this["brandName"] as String,
        imageUrl = this["imageUrl"] as String?,
        minPrice = (this["minPrice"] as Number).toLong(),
        compareAtPrice = (this["compareAtPrice"] as Number?)?.toLong(),
        discountPercent = (this["discountPercent"] as Number).toInt(),
        ratingAvg = (this["ratingAvg"] as Number).toDouble(),
        ratingCount = (this["ratingCount"] as Number).toLong(),
        badges = this["badges"] as List<String>,
        prepMinutes = (this["prepMinutes"] as Number).toInt(),
    )
}
```

> Numbers from a callable arrive as `Integer`, `Long` or `Double` depending on their value. That's why every number goes through `as Number` and then `.toLong()` or `.toInt()`.

### 5.2 Domain

`features/search/domain/model/SearchModels.kt`:

```kotlin
package com.example.dukkanapp.features.search.domain.model

/** 05b.3 sort sheet: Relevance / Price: low to high / Price: high to low / Newest first. */
enum class SearchSort(val wire: String) {
    Relevance("relevance"), PriceAsc("price_asc"), PriceDesc("price_desc"), Newest("newest")
}

data class SearchQuery(
    val text: String,
    val storeId: String,
    val categoryId: String? = null,
    val onSale: Boolean = false,
    val sort: SearchSort = SearchSort.Relevance,
    val page: Int = 0,
)

data class ProductHit(
    val productId: String,
    val name: String,
    val brandName: String,
    val imageUrl: String?,
    val priceMillimes: Long,
    val compareAtMillimes: Long?,
    val discountPercent: Int,
    val ratingAvg: Double,
    val ratingCount: Long,
    val badges: List<String>,
)

data class SearchPage(
    val hits: List<ProductHit>,
    val totalHits: Int,
    val page: Int,
    val hasMore: Boolean,
    val suggestion: String?,
)
```

`features/search/domain/repository/SearchRepository.kt`:

```kotlin
package com.example.dukkanapp.features.search.domain.repository

import com.example.dukkanapp.core.error.AppResult
import com.example.dukkanapp.features.search.domain.model.SearchPage
import com.example.dukkanapp.features.search.domain.model.SearchQuery

interface SearchRepository {
    suspend fun search(query: SearchQuery): AppResult<SearchPage>
    suspend fun trending(): AppResult<List<String>>
}
```

`features/search/data/repository/SearchRepositoryImpl.kt`:

```kotlin
package com.example.dukkanapp.features.search.data.repository

import com.example.dukkanapp.core.error.AppResult
import com.example.dukkanapp.core.error.mapper.AppFailureMapper
import com.example.dukkanapp.features.search.data.datasource.SearchRemoteDataSource
import com.example.dukkanapp.features.search.data.dto.SearchHitDto
import com.example.dukkanapp.features.search.domain.model.ProductHit
import com.example.dukkanapp.features.search.domain.model.SearchPage
import com.example.dukkanapp.features.search.domain.model.SearchQuery
import com.example.dukkanapp.features.search.domain.repository.SearchRepository
import javax.inject.Inject

class SearchRepositoryImpl @Inject constructor(
    private val remote: SearchRemoteDataSource,
    private val failureMapper: AppFailureMapper,
) : SearchRepository {

    override suspend fun search(query: SearchQuery): AppResult<SearchPage> = failureMapper.guardFailure {
        val dto = remote.search(
            query = query.text,
            storeId = query.storeId,
            categoryId = query.categoryId,
            onSale = query.onSale,
            sort = query.sort.wire,
            page = query.page,
        )
        SearchPage(
            hits = dto.hits.map { it.toDomain() },
            totalHits = dto.totalHits,
            page = dto.page,
            hasMore = dto.page + 1 < dto.pageCount,
            suggestion = dto.suggestion,
        )
    }

    override suspend fun trending(): AppResult<List<String>> = failureMapper.guardFailure { remote.trending() }

    private fun SearchHitDto.toDomain() = ProductHit(
        productId = productId,
        name = name,
        brandName = brandName,
        imageUrl = imageUrl,
        priceMillimes = minPrice,
        compareAtMillimes = compareAtPrice,
        discountPercent = discountPercent,
        ratingAvg = ratingAvg,
        ratingCount = ratingCount,
        badges = badges,
    )
}
```

Use the same `guardFailure` signature as the auth repositories. If yours differs, match it.

Bind it in `features/search/di/SearchModule.kt` with `@Binds`, like `AuthModule`. No `@Provides` is needed: `FirebaseFunctions` already comes from `FirebaseModule` (13).

### 5.3 Presentation (MVI)

The rules:
- **Debounce 300 ms:** one call per pause, not per key.
- **Fewer than 2 characters:** don't search. Show recent searches from DataStore (see 00).
- **`collectLatest`:** a new query cancels the call still running, so slow old results never overwrite new ones.
- **Load more:** appends the next page while `hasMore` is true.

`features/search/presentation/logic/SearchContract.kt`:

```kotlin
package com.example.dukkanapp.features.search.presentation.logic

import com.example.dukkanapp.core.common.text.AppUiText
import com.example.dukkanapp.features.search.domain.model.ProductHit
import com.example.dukkanapp.features.search.domain.model.SearchSort

data class SearchUiState(
    val query: String = "",
    val categoryId: String? = null,
    val onSale: Boolean = false,
    val sort: SearchSort = SearchSort.Relevance,
    val hits: List<ProductHit> = emptyList(),
    val totalHits: Int = 0,
    val page: Int = 0,
    val hasMore: Boolean = false,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: AppUiText? = null,
    // 05b.4
    val suggestion: String? = null,
    // 05b.1 (empty query)
    val recentSearches: List<String> = emptyList(),
    val trending: List<String> = emptyList(),
    val recentlyViewed: List<ProductHit> = emptyList(),
) {
    val showEmptyQuery: Boolean get() = query.trim().length < 2
    val showNoResults: Boolean get() = !isLoading && error == null && !showEmptyQuery && hits.isEmpty()
}

sealed interface SearchIntent {
    data class QueryChanged(val query: String) : SearchIntent
    data object QuerySubmitted : SearchIntent                       // keyboard "Search": saves to recent
    data class CategorySelected(val categoryId: String?) : SearchIntent
    data class OnSaleToggled(val onSale: Boolean) : SearchIntent
    data class SortSelected(val sort: SearchSort) : SearchIntent
    data class SuggestionClicked(val text: String) : SearchIntent  // recent, trending, "Did you mean"
    data object ClearRecentSearches : SearchIntent
    data object LoadMore : SearchIntent
    data object Retry : SearchIntent
    data class ProductClicked(val productId: String) : SearchIntent
}

sealed interface SearchEffect {
    data class OpenProduct(val productId: String) : SearchEffect
}
```

`features/search/presentation/logic/SearchViewModel.kt`:

```kotlin
package com.example.dukkanapp.features.search.presentation.logic

import androidx.lifecycle.viewModelScope
import com.example.dukkanapp.core.common.mvi.AppViewModel
import com.example.dukkanapp.core.error.ui.toUiText
import com.example.dukkanapp.features.search.domain.model.SearchQuery
import com.example.dukkanapp.features.search.domain.repository.SearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: SearchRepository,
    // The store comes from the selected delivery address (03, zones → storeId).
    // Replace with your "current store" use case once the location feature exists.
    private val currentStoreId: CurrentStoreIdProvider,
    // Recent searches + recently viewed, both on the phone (DataStore). See § 6.
    private val history: SearchHistoryRepository,
) : AppViewModel<SearchUiState, SearchIntent, SearchEffect>(SearchUiState()) {

    /** Every change that should trigger a new first-page search goes through here. */
    private val searchKey = MutableStateFlow(SearchKey("", null, false, currentState.sort))

    init {
        viewModelScope.launch {
            searchKey
                .debounce(DEBOUNCE_MS)
                .distinctUntilChanged()
                .collectLatest { key -> runSearch(key, page = 0) }
        }
        // 05b.1 sections
        viewModelScope.launch { history.observeRecentSearches().collect { setState { copy(recentSearches = it) } } }
        viewModelScope.launch { history.observeRecentlyViewed().collect { setState { copy(recentlyViewed = it) } } }
        viewModelScope.launch { repository.trending().onSuccess { setState { copy(trending = it) } } }
    }

    override fun onIntent(intent: SearchIntent) {
        when (intent) {
            is SearchIntent.QueryChanged -> {
                setState { copy(query = intent.query) }
                searchKey.value = searchKey.value.copy(text = intent.query.trim())
            }
            SearchIntent.QuerySubmitted -> viewModelScope.launch { history.addRecentSearch(currentState.query.trim()) }
            is SearchIntent.SuggestionClicked -> {
                setState { copy(query = intent.text) }
                searchKey.value = searchKey.value.copy(text = intent.text)
                viewModelScope.launch { history.addRecentSearch(intent.text) }
            }
            SearchIntent.ClearRecentSearches -> viewModelScope.launch { history.clearRecentSearches() }
            is SearchIntent.CategorySelected -> {
                setState { copy(categoryId = intent.categoryId) }
                searchKey.value = searchKey.value.copy(categoryId = intent.categoryId)
            }
            is SearchIntent.OnSaleToggled -> {
                setState { copy(onSale = intent.onSale) }
                searchKey.value = searchKey.value.copy(onSale = intent.onSale)
            }
            is SearchIntent.SortSelected -> {
                setState { copy(sort = intent.sort) }
                searchKey.value = searchKey.value.copy(sort = intent.sort)
            }
            SearchIntent.LoadMore -> loadMore()
            SearchIntent.Retry -> viewModelScope.launch { runSearch(searchKey.value, page = 0) }
            is SearchIntent.ProductClicked -> sendEffect(SearchEffect.OpenProduct(intent.productId))
        }
    }

    private suspend fun runSearch(key: SearchKey, page: Int) {
        if (key.text.length < MIN_CHARS) {
            setState { copy(hits = emptyList(), totalHits = 0, hasMore = false, isLoading = false, error = null, suggestion = null) }
            return
        }
        setState { if (page == 0) copy(isLoading = true, error = null) else copy(isLoadingMore = true) }
        val query = SearchQuery(key.text, currentStoreId(), key.categoryId, key.onSale, key.sort, page)
        repository.search(query)
            .onSuccess { result ->
                setState {
                    copy(
                        hits = if (page == 0) result.hits else hits + result.hits,
                        totalHits = result.totalHits,
                        page = result.page,
                        hasMore = result.hasMore,
                        suggestion = result.suggestion,
                        isLoading = false,
                        isLoadingMore = false,
                    )
                }
            }
            .onFailure { failure ->
                setState { copy(isLoading = false, isLoadingMore = false, error = failure.toUiText()) }
            }
    }

    private fun loadMore() {
        val state = currentState
        if (!state.hasMore || state.isLoading || state.isLoadingMore) return
        viewModelScope.launch { runSearch(searchKey.value, page = state.page + 1) }
    }

    private data class SearchKey(
        val text: String,
        val categoryId: String?,
        val onSale: Boolean,
        val sort: com.example.dukkanapp.features.search.domain.model.SearchSort,
    )

    private companion object {
        const val DEBOUNCE_MS = 300L
        const val MIN_CHARS = 2
    }
}

/** Temporary seam until the location/zone feature provides the cart's store. */
fun interface CurrentStoreIdProvider {
    suspend operator fun invoke(): String
}
```

`onSuccess` and `onFailure` are the `AppResult` helpers from the auth guide. If yours are named differently, use your names. Until the location feature exists, provide `CurrentStoreIdProvider` from a Hilt module that returns `"lac2"`. Delete it once the zone → store lookup is built.

### 5.4 Screen

Use the design-system pieces, so no new UI tokens are needed:
- **Search field:** `AppSearchBar` (Field variant) from `DesignSystem/v4/11`, with `autoFocus`.
- **Chips (05b.2):** "All · Sneakers · Running · Kids · On sale". The category chips come from `categories where parentId == <top department of the results>`; "On sale" sends `OnSaleToggled`.
- **Header:** "128 results for “runner”" from `totalHits`, plus a "Relevance ▾" chip that opens the sort sheet (05b.3). "Show 128 results" closes the sheet.
- **Results:** `LazyVerticalGrid(GridCells.Fixed(2))` of product cards. Show the price with `AppPriceText` (millimes).
- **Load more:** when the last visible index is within 4 of the end, send `SearchIntent.LoadMore`.
- **States:**
  - `showEmptyQuery` → 05b.1 (§ 6).
  - `isLoading` → the skeleton grid (14.5).
  - `showNoResults` → 05b.4 (§ 7).
  - `error` → `AppMessageBanner` with a Retry button.

---

## 6. Empty query screen (05b.1)

| Section | Source | Why there |
|---|---|---|
| Recent searches · "Clear all" | DataStore, last 8 queries, newest first | **Per phone, private.** Nobody else needs them, they must work offline, and the user can clear them with one tap (11.3 "Clear search history" uses the same store) |
| Trending now | `searchSuggestions` callable → Algolia Query Suggestions | Popular queries across all users: only Algolia's analytics knows them |
| Popular categories | `categories` level 0 (already cached for Home) | No extra read |
| Recently viewed | DataStore list of the last 10 product ids (written when 06.1 opens) + `products.whereIn(documentId(), ids)` | **Per phone.** A Firestore copy would cost a write on every product view and is not needed on other devices. Live prices come from the product docs |

`features/search/domain/repository/SearchHistoryRepository.kt`:

```kotlin
package com.example.dukkanapp.features.search.domain.repository

import com.example.dukkanapp.features.search.domain.model.ProductHit
import kotlinx.coroutines.flow.Flow

interface SearchHistoryRepository {
    fun observeRecentSearches(): Flow<List<String>>
    suspend fun addRecentSearch(text: String)
    suspend fun clearRecentSearches()

    /** Ids from DataStore, products from Firestore (cache first). */
    fun observeRecentlyViewed(): Flow<List<ProductHit>>
    suspend fun addRecentlyViewed(productId: String)
}
```

The implementation keeps two DataStore keys, `recent_searches` and `recently_viewed`. Store each list as one string joined with `\n`; queries can't contain a newline because the search field is single-line. Keep at most 8 searches and 10 ids, move a repeated entry to the front, and drop entries under 2 characters. `addRecentlyViewed` is called by the product details ViewModel (06.1).

---

## 7. No results (05b.4)

"No results for “air jordn” · Check the spelling or try a more general word · Did you mean **air runner**? · Popular right now · You might like".

| Part | Source |
|---|---|
| "Did you mean air runner?" | `SearchPage.suggestion` (only when `totalHits == 0`, see § 2 Query Suggestions). Tapping sends `SuggestionClicked("air runner")` |
| Popular right now | The `trending` chips already loaded for 05b.1 |
| You might like | A second `searchProducts` call with an **empty query** in the same store (sorted by `popularityScore` through custom ranking), first 3 hits |

If `suggestion` is null, hide the "Did you mean" line. Algolia didn't find a close query, and we don't invent one.

---

## 8. Costs and limits

| Item | Note |
|---|---|
| Algolia free tier | Enough for development. Check the current plan limits on algolia.com before launch |
| One search = one function call + one Algolia request (two when 0 hits: the "Did you mean" lookup) | Debounce and the 2-character minimum keep this low |
| Query Suggestions | Built from search analytics; needs some traffic before "Trending" is meaningful. Seed manual entries at launch |
| Extension writes | One Algolia write per product write. Batch admin imports are fine |
| Index size | Small records (no description), so well within limits for 12k products |

---

## Check

- [ ] Algolia app (EU) + index `products` + replicas `products_price_asc` / `products_price_desc` / `products_newest`.
- [ ] Query Suggestions index `products_query_suggestions` created (with seed entries); "Trending now" shows 5 chips.
- [ ] "air jordn" returns 0 hits and `suggestion = "air runner"` (if that suggestion exists).
- [ ] "On sale" returns only products with `discountPercent > 0`.
- [ ] Recent searches and recently viewed survive an app restart and are cleared by "Clear all".
- [ ] Index settings match the table in § 2.
- [ ] Extension installed on `products` with the field list from § 3; full index import done once.
- [ ] `ALGOLIA_SEARCH_KEY` secret set (search-only key), `ALGOLIA_APP_ID` in `.env`.
- [ ] `searchProducts` deployed in `europe-west1`; a signed-out call returns `unauthenticated`.
- [ ] Searching "air runer" (typo) finds "Air Runner 2".
- [ ] A product with `isActive = false` never appears.
- [ ] A product not in the cart store's `inStockStoreIds` never appears.
- [ ] Typing fast sends one request after you stop (check the Functions logs).
- [ ] Load more appends page 2 without duplicates; Retry works after turning airplane mode off.
