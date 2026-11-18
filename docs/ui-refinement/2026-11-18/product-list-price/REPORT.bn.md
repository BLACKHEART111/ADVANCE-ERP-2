# Product list — price layout

সমস্যা: category, SKU ও variant count-এর unweighted Row মূল Row-এর প্রস্থ প্রায় সম্পূর্ণ নিয়ে নিচ্ছিল। ফলে দাম অক্ষরে অক্ষরে ভেঙে card অস্বাভাবিক লম্বা হচ্ছিল। before-reported.jpg-এ সমস্যাটি দেখা যায়।

পরিবর্তন: metadata-র জন্য full-width FlowRow ব্যবহার করা হয়েছে, যাতে প্রতিটি item প্রয়োজনমতো পরের লাইনে যায়। মূল্য আলাদা full-width লাইনে ডানদিকে থাকে। এক লাইনের money text-এ wrapping বন্ধ এবং সীমার অতিরিক্ত বড় অঙ্কের জন্য ellipsis আছে। বাংলা interface-এ variant count বাংলায় দেখানো হয়। অর্থের হিসাব, formatter, data loading বা card navigation বদলায়নি।

ফাইল: app/src/main/java/com/pocketdev/advance_erp/feature/product/ProductListScreen.kt।

যাচাই: assembleDebug পাস; APK signature verification পাস; source diff --check পাস। UI-only সংশোধনে নতুন unit test প্রয়োজন হয়নি। ব্যবহারকারীর নির্দেশ অনুযায়ী ADB ব্যবহার করা হয়নি; rendered after screenshot যাচাই করা হয়নি।
