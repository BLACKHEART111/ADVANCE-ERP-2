# Android 16 KB compatibility সংশোধন

প্রকৃত source: ADVANCE-ERP-2। তারিখ: ১৮ নভেম্বর ২০২৬।

## কারণ ও পরিবর্তন

CameraX 1.3.4-এর `libimage_processing_util_jni.so`-এর LOAD alignment ছিল 4096 bytes। APK zip alignment ঠিক থাকলেও ELF সমস্যাটি ছিল। CameraX camera-camera2, camera-lifecycle ও camera-view একসঙ্গে 1.4.2 করা হয়েছে। Bundled offline ML Kit 17.3.0 অপরিবর্তিত। পুরোনো barcode library-র LOAD alignment আগেই 16384 ছিল; popup-এর তার “Unknown error” বার্তাকে ELF alignment ব্যর্থতা হিসেবে ধরা হয়নি।

Warning লুকানোর setting বা debug বন্ধ করা হয়নি। Business logic ও database schema এই পরিবর্তনে বদলানো হয়নি।

## ফাইল

- app/build.gradle.kts: তিনটি CameraX dependency 1.4.2।
- scripts/check_apk_16kb.py: APK-র native ELF LOAD, RELRO এবং uncompressed ZIP offset checker।
- app/src/androidTest/java/com/pocketdev/advance_erp/NativeCompatibilityTest.kt: library load, camera provider ও barcode decoding smoke tests।
- app/src/androidTest/assets/native-smoke-qr.txt: Python qrcode দিয়ে তৈরি স্বাধীন fixture।

## যাচাই

- assembleDebug ও testDebugUnitTest: সফল; ৩০৫ tests, ০ failures, ০ errors।
- তিনটি arm64 native library: LOAD alignment 16384, RELRO ও ZIP offset পরীক্ষা পাস। signed APK-ও পুনরায় পরীক্ষা করা হয়েছে।
- zipalign -c -P 16 -v 4: Verification successful।
- আগের public debug signing certificate দিয়েই APK signed। Install-এর আগে private data backup রাখা হয়েছে। কোনো uninstall বা data clear command চালানো হয়নি।
- ফোনে প্রথম update-এর পর app-open.png সরাসরি dashboard দেখিয়েছে; Android App Compatibility dialog নেই।
- প্রথম native run-এ library loading ও camera provider পাস। অ্যাপের বিদ্যমান QR generator-এর তৈরি পরীক্ষার ছবি decode হয়নি। এই পৃথক সমস্যার প্রমাণ device-tests-app-qr.log-এ রাখা হয়েছে; generator বদলানো হয়নি। স্বাধীন QR fixture দিয়ে scanner পরীক্ষা পাস; চূড়ান্ত run-এ তিনটি device test-ই পাস (device-tests.log)।
- ফোনের actual page size 4096 bytes: এটি সত্যিকারের 16 KB hardware runtime test নয়।

## ডিভাইসের বাহ্যিক পরিবর্তন

পরীক্ষার মধ্যে package ফোন থেকে অদৃশ্য হয়। Android logcat 17:44:36-এ uninstallSource=com.android.launcher দেখায়। আমাদের command history-তে uninstall নেই। এই কারণে পরবর্তী screenshot lock screen দেখিয়েছে; সেটিকে app verification হিসেবে গণনা করা হয়নি। ব্যবহারকারী নিজে uninstall করেছেন বলে নিশ্চিত করেছেন এবং আবার install করতে বলেছেন। সেই নির্দেশে পুনরায় install ও test করা হয়েছে। app-final.png-এ warning ছাড়া dashboard দেখা গেছে। আগের backup সংরক্ষিত আছে; ব্যবহারকারীর ডেটা পুনরুদ্ধার স্বয়ংক্রিয়ভাবে করা হয়নি।

## সূত্র

[Android 16 KB page sizes](https://developer.android.com/guide/practices/page-sizes)

[CameraX release notes](https://developer.android.com/jetpack/androidx/releases/camera)

[ML Kit release notes](https://developers.google.com/ml-kit/release-notes)
