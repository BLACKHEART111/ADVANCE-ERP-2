# Premium More Screen ও Bottom Navigation

২১ নভেম্বর ২০২৬

## পরিবর্তন

- More Screen-এ একই `LazyVerticalGrid`-এর মধ্যে banner, section title ও সব ১০টি বিদ্যমান module card রাখা হয়েছে। স্ক্রল করলে banner এবং grid একসঙ্গে যায়; bottom navigation স্থির থাকে। সরু প্রস্থে grid adaptive হয়।
- “আরও” banner title এক লাইনে রাখা হয়েছে এবং banner-এর উচ্চতা/illustration কমানো হয়েছে। decorative wave ও cube Compose Canvas-এ আঁকা হয়; network asset নেই।
- shared five-tab bottom navigation-এ rounded surface, selected purple gradient এবং screen-reader selected semantics যোগ হয়েছে। routes ও save/restore tab navigation অপরিবর্তিত।
- Central Ledger ও Business Control Center আগে একই route state-এ একই screen খুলত। এখন Central Ledger Statements/customer-ledger state দিয়ে শুরু হয়, Business Control Center summary-তে শুরু হয়; দুটির route ID বজায় আছে।
- English ও Bangla banner subtitle এবং icon accessibility label যোগ হয়েছে।

## যাচাই

- `:app:assembleDebug`, `:app:testDebugUnitTest`, `:app:assembleDebugAndroidTest` — সফল; Gradle 8.13 ও pinned project dependencies।
- আলাদা `com.pocketdev.advance_erp2.premiumqa` package-এ `PremiumUiRegressionTest#moreHubShowsEveryModuleAndOpensItsRealRoute` — AndroidJUnitRunner-এ `OK (1 test)`। Test confirms all ten labels can be scrolled into view and Settings opens route `settings`.
- `git diff --check` — সফল।
- Bangla/English এবং Light/Dark-এ More Screen screenshot নেওয়া হয়েছে।

প্রথম Gradle `connectedDebugAndroidTest` invocation test শেষ হওয়ার পরে Android test plugin-এর cleanup hook-এ `/root/android-sdk/build-tools/35.0.0/aapt` না পাওয়ায় nonzero exit দিয়েছিল। একই test APK ও QA app-এ সরাসরি `adb shell am instrument` চালিয়ে test pass নিশ্চিত করা হয়েছে।

পুরো ব্যবসায়িক workflow regression বা অন্য module-এর end-to-end পরীক্ষা এই UI pass-এর অংশ নয়। Production application package-এ APK install করা হয়নি; QA verification-এ isolated `.premiumqa` package ব্যবহার করা হয়েছে।

## Screenshot

- `bn-light.png`
- `en-light.png`
- `bn-dark.png`
- `en-dark.png`
