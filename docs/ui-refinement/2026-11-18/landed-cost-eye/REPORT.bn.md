# Inventory ও product slider — cost reveal এবং landed buy price

## আচরণ

- Inventory stock ও low-stock card-এ eye button দিয়ে বিদ্যমান cost/landed range দেখা বা লুকানো যায়। শুরুতে খরচ ঢাকা থাকে।
- Product detail-এর cost tile, variant list এবং fullscreen photo slider-এও eye toggle আছে। Slider-এর variant বদলালে খরচ আবার ঢাকা হয়।
- Buy price UI সর্বশেষ received inventory cost-layer-এর unitLandedCostBdtMinor ব্যবহার করে। Variant-এর জন্য ঠিক সেই variant-এর record লাগে; অন্য variant-এর cost ধার নেয় না। Base product slide পুরো product-এর সর্বশেষ receipt-এর reference cost দেখায়।
- Inventory card আগের active-layer min/max range দেখায়; slider একটি reference buy price হিসেবে latest receipt cost দেখায়। এটি weighted average বা FIFO COGS বদলানো নয়।
- Receipt date, createdAt ও id দিয়ে স্থির ক্রমে latest record বাছাই হয়। Sold-out receipt reference হিসেবে থাকে; নিষ্ক্রিয় অথচ অবশিষ্ট stock-সহ layer ব্যবহার হয় না। Valid zero-cost receipt-ও গ্রহণ করে।
- Landed history না থাকলে variant/master purchase price fallback হয়। Landed cost BDT-তে দেখানো হয়।
- Cost-layer Flow observe করায় cost adjustment/receipt update হলে detail UI নতুন value পায়।
- Existing canViewCost permission মেনে cost button ও value দেখানো হয়; product screen-এও role-derived permission দেওয়া হয়েছে।
- Eye toggle local UI state; source accounting records, master prices, FIFO, COGS, repository writes ও financial calculations অপরিবর্তিত। এটি buy-price display-এর স্বয়ংক্রিয় উৎস, master purchase price overwrite নয়।

## ফাইল

- core/util/LandedCostDisplay.kt: read-only landed-cost selection।
- feature/inventory/InventoryScreen.kt ও InventoryViewModel.kt।
- feature/product/ProductDetailDialog.kt, VariantPhotoSliderDialog.kt, ProductListScreen.kt ও ProductViewModel.kt।
- LandedCostDisplayTest.kt: newest receipt, variant/product isolation, sold-out/disabled layers, zero/missing cost এবং stable selection regression।

## যাচাই

assembleDebug সফল; 316 unit tests পাস (০ failures/errors)। Signed APK verification পাস। ব্যবহারকারীর নির্দেশ অনুযায়ী ADB ব্যবহার বা rendered screenshot verification করা হয়নি।
