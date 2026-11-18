# Landed-cost eye buttons — biometric verification

## পরিবর্তন

Inventory stock/low-stock cost eye, product detail cost tile/variant cost eye এবং fullscreen photo slider-এর cost eye এখন সরাসরি value প্রকাশ করে না। নতুন authentication attempt সফল হওয়ার পরেই সেই cost view প্রকাশ হয়। Hide করতে authentication লাগে না; পুনরায় reveal করতে আবার যাচাই লাগে।

- Existing canViewCost permission আগে বহাল থাকে।
- Strong system biometric prompt ব্যবহার করা হয়েছে; system উপযুক্ত strong modality নির্বাচন করে (সাধারণত fingerprint; strong face-ও OS সমর্থন করলে গ্রহণযোগ্য)। এটি fingerprint-only modality selector নয়। Android-এর app PIN/device PIN fallback এই cost gate-এ দেওয়া হয়নি।
- ফোনে biometric setup না থাকলে বা unavailable হলে তথ্য ঢাকা থাকে এবং ব্যবহারকারীকে কারণ জানায়।
- Cancel, error, unmatched fingerprint ও prompt চালু হতে ব্যর্থ হলে cost দেখা যায় না।
- Background/ON_STOP, scope disposal, product/slide বদল বা permission বদল হলে disclosure state invalid হয়; pending prompt cancel হয়। পুরোনো asynchronous success নতুন scope unlock করতে পারে না।
- Rotation বা app restart-এ verification state save করা হয় না।
- App-এর বিদ্যমান login/app-lock biometric settings ও PIN logic বদলানো হয়নি; eye disclosure-এর যাচাই আলাদা।
- ব্যবহারকারীর অতিরিক্ত নির্দেশে Product Cost History/FIFO Layers ও Cost Adjustment dialog-ও fingerprint gate-এর ভেতরে রাখা হয়েছে। Authentication ছাড়া summary, layer details, original price, expense, valuation এবং adjustment controls compose হয় না।
- Inventory Valuation, Reconciliation এবং COGS Trace tab-এর cost data/export controls-এও একই gate বসানো হয়েছে। অন্য accounting/report module-এ global policy পরিবর্তন করা হয়নি।
- Data, landed-cost selection, FIFO, COGS ও financial calculations অপরিবর্তিত।

## ফাইল

- core/security/CostRevealGate.kt: disclosure state ও stale-attempt invalidation।
- core/security/BiometricCostVisibility.kt: shared Compose/system prompt integration।
- feature/inventory/InventoryScreen.kt।
- feature/product/ProductDetailDialog.kt এবং VariantPhotoSliderDialog.kt।
- core/security/BiometricCostGuard.kt ও inventory/costhistory/ProductCostHistoryDialog.kt: locked panel ও cost-layer gate।
- CostRevealGateTest.kt: denied role, success, error/cancel, background/disposal, re-authentication এবং overlapping/stale callbacks।

## যাচাই

Cost-layer guard-সহ final assembleDebug সফল; 322 unit tests পাস, failures/errors শূন্য। Final signed APK verification সফল। ব্যবহারকারীর নির্দেশ মেনে ADB ব্যবহার করা হয়নি। বাস্তব fingerprint sensor, prompt rendering, success/cancel এবং background flow ফোনে হাতে যাচাই বাকি।

## Android API সূত্র

[Biometric authentication](https://developer.android.com/identity/sign-in/biometric-auth) — allowed authenticator configuration।

[BiometricPrompt](https://developer.android.com/reference/androidx/biometric/BiometricPrompt) — authentication callbacks ও cancelAuthentication।
