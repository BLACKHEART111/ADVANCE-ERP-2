# Add Product — variant card layout

## সমস্যা

রং/সাইজ ও ক্রয়/বিক্রয় মূল্য একই unweighted Row-তে ছিল। রং/সাইজ প্রথমে জায়গা নেওয়ায় পরের price Text-এর জন্য প্রায় এক অক্ষরের প্রস্থ থাকত। বাংলা লেখা অক্ষরে অক্ষরে ভেঙে card-এর উচ্চতা অস্বাভাবিক বেড়ে যাচ্ছিল। ব্যবহারকারীর screenshot: before-reported.jpg।

## পরিবর্তন

- দামকে header-এর নিচে card-এর পূর্ণ প্রস্থে আলাদা line-এ রাখা হয়েছে; প্রয়োজন হলে স্বাভাবিকভাবে একাধিক line হবে।
- রং/সাইজ নিজস্ব text block-এ আছে; বাংলা interface-এ বাংলা label দেওয়া হয়েছে।
- edit/delete-এর touch target 32dp থেকে 48dp করা হয়েছে; icon 18dp রাখা হয়েছে।
- quantity, +10/+50/+100, edit/delete callbacks, validation এবং save logic বদলানো হয়নি।

পরিবর্তিত source: app/src/main/java/com/pocketdev/advance_erp/feature/product/AddProductDialog.kt।

## যাচাই

- :app:assembleDebug সফল (3m 57s)।
- git diff --check: source whitespace পরীক্ষা পাস।
- আগের signing key দিয়ে signed APK তৈরি এবং update install সফল।
- ব্যবহারকারী নিজে ইনস্টল করে visual পরীক্ষা করবেন বলে ADB পরীক্ষা বন্ধ করতে বলেছেন। Updated variant card-এর rendered before/after পরীক্ষা সম্পূর্ণ হয়নি; pixel-perfect দাবি করা হচ্ছে না।
- UI-only সংশোধন; quantity/save-এর business logic বদলায়নি। এই পরিবর্তনের জন্য নতুন unit test যোগ করা হয়নি।
