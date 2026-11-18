# Inventory variant photo সংশোধন — ১৮ নভেম্বর ২০২৬

Project: `/data/user/0/com.termux/files/home/ADVANCE-ERP-2`। আগের Import/PO সংশোধনগুলো এই build-এ রয়েছে।

## সমস্যা

ছোট gallery-তে BLACK/S-এর সঠিক ছবি দেখা গেলেও fullscreen slider পুরোনো base ছবি দেখাচ্ছিল। Variant image-গুলো পরে load হতো; fullscreen-এর `remember`-করা slide list-এ mutable map object একই থাকায় সেই তালিকা নতুন photo URI নিত না। ছবি না থাকা variant-এ selected base/hero URI-ও fallback হিসেবে ব্যবহার করা হচ্ছিল।

## সম্মত photo rule

1. Variant-এর নিজের ছবি থাকলে সেটিই।
2. নিজের ছবি না থাকলে একই product-এর একই color-এর অন্য size-এর ছবি।
3. সেই color-এর কোনো ছবিই না থাকলে ‘ছবি নেই’ placeholder।

অন্য color, অন্য product বা base photo স্বয়ংক্রিয় fallback হবে না। Explicit নিজের photo সবসময় অগ্রাধিকার পাবে। Case ও বাইরের whitespace উপেক্ষা করে color মেলানো হয়।

## পরিবর্তন

- ছোট gallery, variant list ও fullscreen-এর জন্য একই resolved photo mapping।
- Async photo load-এ fullscreen-এর stale slide cache বাদ দেওয়া।
- Product বদলালে image state আলাদা; pager-এ slide ID দিয়ে stable key।
- Database-এ photo assignment বা stock/price বদলানো হয়নি; একই color-এর ছবি display-এর সময় ভাগ করা হয়।

ফাইল:

- `core/util/VariantPhotoResolver.kt`
- `feature/product/ProductDetailDialog.kt`
- `feature/product/VariantPhotoSliderDialog.kt`
- `test/.../VariantPhotoResolverTest.kt`

## যাচাই

- `assembleDebug`: সফল।
- ৬০টি suite-এ **৩০৫টি unit test পাস**; failure/error/skipped = ০।
- নতুন ৮টি regression test: own-photo priority, same-color size inheritance, missing-color placeholder, case/whitespace, cross-product isolation, unspecified color, empty own URI এবং asynchronous snapshot update।
- `git diff --check` পাস।
- মূল app-এর একই signing certificate-এ debug APK sign করা হয়েছে।
- ADB সংযুক্ত নেই; এই photo fix-এর নতুন APK ফোনে install/render করে যাচাই করা হয়নি।

Evidence: `build.log`, `test-summary.json`, `apk-signature.txt`।

## App খোলার compatibility warning

আগের APK-এর native ELF header সরাসরি পরীক্ষা করা হয়েছে (`native-alignment.json`):

- `libimage_processing_util_jni.so`: LOAD segment alignment **4096 bytes**, অর্থাৎ 4 KB। এটি 16 KB requirement পূরণ করছে না।
- `libbarhopper_v3.so`: LOAD segment alignment **16384 bytes**, অর্থাৎ 16 KB। Screenshot-এর “Unknown error”-এর কারণ এই ফল দিয়ে নিশ্চিত করা যায় না; আলাদা runtime যাচাই প্রয়োজন।

দুটি library-র APK ZIP offset-ও 16 KB aligned পাওয়া গেছে। তাই শুধু APK zip alignment বা warning লুকিয়ে camera library-র ELF সমস্যাটি সমাধান হবে না। Compatible native library version প্রয়োজন। Debug build-এ Android এই সতর্কবার্তা দেখাচ্ছে। Photo fix-এর build-এ native dependency পরিবর্তন করা হয়নি।

[Android-এর 16 KB নির্দেশনা](https://developer.android.com/guide/practices/page-sizes)।

## APK

`/data/user/0/com.termux/files/home/ADVANCE-ERP-builds/variant-photo-20261118/ADVANCE-ERP-variant-photo-debug.apk`

একই folder-এ `SHA256SUMS.txt` আছে।
