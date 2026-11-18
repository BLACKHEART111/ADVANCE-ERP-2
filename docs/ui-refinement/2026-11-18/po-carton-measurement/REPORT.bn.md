# PO packing — optional carton measurement

## প্রয়োজন ও পরিবর্তন

প্রতি CTN-এর মাপ এক ঘরে length*width*height (cm) দেওয়া যায়। 40*50*40 সেমি হলে স্বয়ংক্রিয়ভাবে CBM: 0.080 m³ / CTN দেখায়। cm³ থেকে m³ পেতে গুণফলকে 1,000,000 দিয়ে ভাগ করা হয়। BigDecimal ব্যবহার করা হয়েছে, floating-point rounding নয়।

- মাপ সম্পূর্ণ ঐচ্ছিক; খালি রেখে আগের মতো save করা যায়।
- *, ×, x, X separator, দশমিক, whitespace এবং বাংলা অঙ্ক গ্রহণ করে।
- অসম্পূর্ণ/শূন্য/ঋণাত্মক মাপে inline error হয়; save-এ সংশোধন বা ঘরটি খালি করতে বলে।
- মাপ মুছে দিলে সংশ্লিষ্ট dimensions ও calculated CBM-ও মুছে যায়।
- বিদ্যমান carton dimensions edit-এ prefill হয়; বৈধ মাপ থাকলে CBM সেই মাপ থেকে হিসাব হয়। Dimension ছাড়া পুরোনো CBM থাকলে ব্যবহারকারী field না বদলালে তা রাখা হয়।
- Single ও mixed carton একই field ব্যবহার করে; CBM এক physical CTN-এর জন্য, variant বা PCS সংখ্যা দিয়ে আবার গুণ করা হয় না।
- বিদ্যমান carton draft/persistence fields ব্যবহার করায় schema migration লাগেনি। Save ও duplicate বিদ্যমান length/width/height/CBM বহন করে। কোনো financial calculation বদলানো হয়নি।

## ফাইল

- app/src/main/java/com/pocketdev/advance_erp/feature/imports/CreatePurchaseOrderDialog.kt
- app/src/main/java/com/pocketdev/advance_erp/core/util/CartonMeasurement.kt
- app/src/test/java/com/pocketdev/advance_erp/CartonMeasurementTest.kt

## যাচাই

Build সফল; 310 unit tests পাস, failures/errors শূন্য। APK signature verification পাস। ব্যবহারকারীর নির্দেশে ADB ব্যবহার করা হয়নি; rendered visual verification করা হয়নি।
