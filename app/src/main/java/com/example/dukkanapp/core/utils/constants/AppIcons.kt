package com.example.dukkanapp.core.utils.constants

import androidx.annotation.DrawableRes
import com.example.dukkanapp.R

/**
 * Dukkan icon set (Lucide, 24 grid, 2 px stroke). Tint is always applied by the caller.
 *
 * Usage: `Icon(painterResource(AppIcons.Mail), contentDescription = null, tint = AppTheme.colors.textTertiary)`
 */
object AppIcons {
    // Navigation and app bar
    @DrawableRes
    val House = R.drawable.ic_lucide_house

    @DrawableRes
    val LayoutGrid = R.drawable.ic_lucide_layout_grid

    @DrawableRes
    val ShoppingBag = R.drawable.ic_lucide_shopping_bag

    @DrawableRes
    val Percent = R.drawable.ic_lucide_percent

    @DrawableRes
    val UserRound = R.drawable.ic_lucide_user_round

    @DrawableRes
    val UserPlus = R.drawable.ic_lucide_user_plus

    @DrawableRes
    val ArrowLeft = R.drawable.ic_lucide_arrow_left

    @DrawableRes
    val ArrowRight = R.drawable.ic_lucide_arrow_right

    @DrawableRes
    val ChevronRight = R.drawable.ic_lucide_chevron_right

    @DrawableRes
    val ChevronDown = R.drawable.ic_lucide_chevron_down

    @DrawableRes
    val X = R.drawable.ic_lucide_x

    // Search and filters
    @DrawableRes
    val Search = R.drawable.ic_lucide_search

    @DrawableRes
    val ScanLine = R.drawable.ic_lucide_scan_line

    @DrawableRes
    val SlidersHorizontal = R.drawable.ic_lucide_sliders_horizontal

    @DrawableRes
    val SearchX = R.drawable.ic_lucide_search_x

    @DrawableRes
    val Mic = R.drawable.ic_lucide_mic

    // Auth and account
    @DrawableRes
    val Mail = R.drawable.ic_lucide_mail

    @DrawableRes
    val Lock = R.drawable.ic_lucide_lock

    @DrawableRes
    val Eye = R.drawable.ic_lucide_eye

    @DrawableRes
    val EyeOff = R.drawable.ic_lucide_eye_off

    @DrawableRes
    val Phone = R.drawable.ic_lucide_phone

    @DrawableRes
    val Bell = R.drawable.ic_lucide_bell

    @DrawableRes
    val Globe = R.drawable.ic_lucide_globe

    @DrawableRes
    val Languages = R.drawable.ic_lucide_languages

    @DrawableRes
    val Sun = R.drawable.ic_lucide_sun

    @DrawableRes
    val Moon = R.drawable.ic_lucide_moon

    @DrawableRes
    val Settings = R.drawable.ic_lucide_settings

    @DrawableRes
    val LogOut = R.drawable.ic_lucide_log_out

    @DrawableRes
    val CircleHelp = R.drawable.ic_lucide_circle_help

    @DrawableRes
    val Camera = R.drawable.ic_lucide_camera

    @DrawableRes
    val Pencil = R.drawable.ic_lucide_pencil

    // Status
    @DrawableRes
    val Info = R.drawable.ic_lucide_info

    @DrawableRes
    val CircleAlert = R.drawable.ic_lucide_circle_alert

    @DrawableRes
    val CircleCheck = R.drawable.ic_lucide_circle_check

    @DrawableRes
    val Check = R.drawable.ic_lucide_check

    @DrawableRes
    val RefreshCw = R.drawable.ic_lucide_refresh_cw

    @DrawableRes
    val WifiOff = R.drawable.ic_lucide_wifi_off

    // Commerce
    @DrawableRes
    val Heart = R.drawable.ic_lucide_heart

    @DrawableRes
    val Plus = R.drawable.ic_lucide_plus

    @DrawableRes
    val Minus = R.drawable.ic_lucide_minus

    @DrawableRes
    val Trash2 = R.drawable.ic_lucide_trash_2

    @DrawableRes
    val Share2 = R.drawable.ic_lucide_share_2

    @DrawableRes
    val Receipt = R.drawable.ic_lucide_receipt

    @DrawableRes
    val CreditCard = R.drawable.ic_lucide_credit_card

    @DrawableRes
    val Banknote = R.drawable.ic_lucide_banknote

    @DrawableRes
    val Wallet = R.drawable.ic_lucide_wallet

    @DrawableRes
    val TicketPercent = R.drawable.ic_lucide_ticket_percent

    @DrawableRes
    val Tag = R.drawable.ic_lucide_tag

    @DrawableRes
    val Gift = R.drawable.ic_lucide_gift

    @DrawableRes
    val Sparkles = R.drawable.ic_lucide_sparkles

    @DrawableRes
    val Zap = R.drawable.ic_lucide_zap

    @DrawableRes
    val Star = R.drawable.ic_lucide_star

    // Ratings: tint with AppTheme.colors.gold. Lucide's site only ships the outline star, so if
    // Figma's export isn't handy: copy ic_lucide_star.xml to ic_lucide_star_filled.xml and add
    // android:fillColor="#FF000000" to the path (the tint colours it, like every other icon).
    @DrawableRes
    val StarFilled = R.drawable.ic_lucide_star_filled

    @DrawableRes
    val Award = R.drawable.ic_lucide_award

    @DrawableRes
    val Flame = R.drawable.ic_lucide_flame

    @DrawableRes
    val Store = R.drawable.ic_lucide_store

    // Delivery and location
    @DrawableRes
    val MapPin = R.drawable.ic_lucide_map_pin

    @DrawableRes
    val LocateFixed = R.drawable.ic_lucide_locate_fixed

    @DrawableRes
    val Truck = R.drawable.ic_lucide_truck

    @DrawableRes
    val Package = R.drawable.ic_lucide_package

    @DrawableRes
    val PackageCheck = R.drawable.ic_lucide_package_check

    @DrawableRes
    val Clock = R.drawable.ic_lucide_clock

    @DrawableRes
    val Calendar = R.drawable.ic_lucide_calendar

    @DrawableRes
    val Navigation = R.drawable.ic_lucide_navigation

    @DrawableRes
    val Route = R.drawable.ic_lucide_route

    @DrawableRes
    val Bike = R.drawable.ic_lucide_bike

    @DrawableRes
    val MessageCircle = R.drawable.ic_lucide_message_circle

    // Categories
    @DrawableRes
    val Shirt = R.drawable.ic_lucide_shirt

    @DrawableRes
    val Headphones = R.drawable.ic_lucide_headphones

    @DrawableRes
    val Watch = R.drawable.ic_lucide_watch

    @DrawableRes
    val Smartphone = R.drawable.ic_lucide_smartphone

    @DrawableRes
    val Laptop = R.drawable.ic_lucide_laptop

    @DrawableRes
    val Sofa = R.drawable.ic_lucide_sofa

    @DrawableRes
    val Footprints = R.drawable.ic_lucide_footprints

    @DrawableRes
    val Backpack = R.drawable.ic_lucide_backpack

    @DrawableRes
    val Glasses = R.drawable.ic_lucide_glasses

    // Brand logos: never tint (use Color.Unspecified)
    @DrawableRes
    val Google = R.drawable.ic_google

    @DrawableRes
    val Facebook = R.drawable.ic_facebook

    @DrawableRes
    val WhatsApp = R.drawable.ic_whatsapp
}