# Baki — budget that fills itself from your notifications

Native Android app (Kotlin + Jetpack Compose). It reads bank and e-wallet notifications on your phone
(Maybank/MAE, TNG, GrabPay, Wise, CIMB…), pops up a review card with the amount, merchant, account and a
suggested category, and adds it to **Activity** when you tap Save.

Everything stays on the phone (local Room database, no internet permission).

## Screens

| Tab | What it does |
| --- | --- |
| **Home** | Left to spend = net salary − paid commitments − this month's spending. Available today, commitments due, budget balance per category, recent activity. |
| **Activity** | Month switcher → search → Category / Account / Amount / Sort filters → day headers with totals → transactions. Tap one to edit or delete, **+** to add manually. |
| **Commitments** | Monthly loans, rent, bills, subscriptions. Tick **Paid** and it comes off this month's salary on Home. Ticks reset each month. |
| **Plan** | Enter net salary. Commitments come off first, then split the rest by category in **RM or %** (manual, no rules). |
| **Stats** | Last 4 months, daily spending, where it went, top merchants, spending by account. |

## Get the APK (no computer needed)

1. Create an empty **private** GitHub repo and upload this whole folder (keep the `.github` folder).
2. GitHub builds it automatically (Actions tab, ~5 minutes).
3. On your phone open the repo → **Releases → Latest build → Baki.apk** and install it.
   Every new push rebuilds it, and it installs over the old version without losing data.

Or open the folder in Android Studio and press Run.

## First-run setup on the phone

1. Open Baki → Home shows **Turn on notification access** → tap it → switch on **Baki**.
2. **Switch greyed out?** That's Android's protection for apps not installed from the Play Store.
   Go to *Settings → Apps → Baki → ⋮ (top right) → Allow restricted settings*, then try step 1 again.
3. Allow Baki's own notifications when asked.
4. Optional: *Settings (gear) → Display over other apps → Turn on* so the review card pops up on top of
   whatever app you're using.
5. *Settings → Send a test notification* to see the flow without spending money.

## How capture works

- `MoneyNotificationListener` receives every notification → `NotificationParser` checks for a money
  amount (RM / MYR, or SGD converted at the rate in Settings) and payment words, and ignores promos and TAC/OTP
  messages.
- Merchant is cleaned ("TOYYIBPAY Oct 18:10:05" → "Toyyibpay"). Paying GrabPay/TNG/Wise from your bank
  is marked as a **Transfer** so it isn't counted as spending twice.
- The category is guessed from the merchant name. When you correct it, Baki remembers that merchant.
- The same amount arriving twice within 3 minutes (bank + wallet) is captured once.
- If an amount matches an unpaid commitment (e.g. Car loan RM650), the card offers **Mark paid** instead,
  so it doesn't come off your salary twice.
- Chat apps (WhatsApp, Telegram…) and SMS start **ignored**. Turn any app on or rename its account in Settings.

## Known limits

- Android only. iPhones don't let apps read other apps' notifications.
- Only notifications that arrive while Baki has access are captured (no history import).
- Foreign currencies other than SGD are shown unconverted. Fix the amount in the card.
- Android 15 may block the over-other-apps popup in some cases. The heads-up notification with
  **Save / Review / Ignore** always works.

## Code map

```
app/src/main/java/com/mohdaie/baki/
  parser/     NotificationParser, CategoryGuesser (unit-tested in app/src/test)
  service/    MoneyNotificationListener, Notifier, QuickActionReceiver
  data/       Room entities, DAO, Repository
  ui/         MainActivity (tabs), ReviewActivity (popup), screens/, sheets/, components/
```
