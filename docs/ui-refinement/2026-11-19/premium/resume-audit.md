# প্রিমিয়াম UI চেকপয়েন্টের পরের অডিট

২০ নভেম্বর ২০২৬, ঢাকা। ভিত্তি main-এর 5189cac।

## সোর্সে করা পরিবর্তন

- PO সম্পাদকের জন্য সংরক্ষণযোগ্য অর্ডার আইডি একবারই নির্ধারিত হয়। আইটেম, কার্টন, খরচ, রিসিভিং ও ফ্রেইটের প্রথম ফলাফল পাওয়ার পর সম্পাদক খোলে। শুধু ধাপ বদলালে সম্পাদনার সতর্কতা আসে না।
- প্যাকিং ও রিসিভিং স্ক্রিন নিজ নিজ PO বা শিপমেন্ট আইডি সংরক্ষণ করে। Android Back-সহ রিসিভিং থেকে বের হওয়ার পথ খসড়া সংরক্ষণ, বাদ দেওয়া ও সম্পাদনা চালানোর ডায়ালগ ব্যবহার করে।
- ফিজিক্যাল কাউন্ট ও কার্টন-ভিত্তিক রিসিভিংয়ের ইনপুট পৃথক থাকে। বর্তমান প্রত্যাশিত সারির সঙ্গে অমিল হলে খসড়ার লেখা রেখে পোস্টিং আটকে যায়। পরিমাণ শূন্য হয়ে যাওয়া PO সারি ফিজিক্যাল কাউন্টে বাদ পড়ে।
- রিসিভিং তৈরি ও সঙ্গে সঙ্গে পোস্ট একটি Room transaction-এ হয়। পোস্ট ব্যর্থ হলে নতুন রসিদও রোলব্যাক হয়। বন্ধ বা বাতিল PO-এর পুরোনো রসিদ পোস্ট করা যায় না। এই পথগুলোর জন্য in-memory SQLite রিগ্রেশন টেস্ট যোগ হয়েছে।
- প্যাকিং ও রিসিভিংয়ের স্থানীয় খসড়া লেখা IO dispatcher-এ চলে। সংরক্ষণের সময় কর্ম বোতাম নিষ্ক্রিয় থাকে। শিপমেন্ট থেকে মিশ্র কার্টনের পণ্যগুলো এক পণ্য হিসেবে সংরক্ষণের পুরোনো fallback সরানো হয়েছে।
- ড্রয়ার ফুটার, স্থির নেভি শিরোনামের টিল আইকন এবং Sales-এর সার্চ, ফিল্টার ও কয়েকটি সারফেসের Light/Dark রঙ সংশোধিত হয়েছে। ড্যাশবোর্ডের ক্রয় কার্ডের ডার্ক কনট্রাস্ট ও মেট্রিক ব্যাজের বাংলা লেখা ঠিক হয়েছে। থিমের primary foreground/background contrast Light-এ 8.37:1 ও Dark-এ 7.63:1। স্থির নেভি পটভূমিতে ব্যবহৃত হালকা টিলের contrast 5.57:1।

- পূর্ণপর্দা সম্পাদক Android উইন্ডোর প্রাপ্ত আকার অনুযায়ী মাপ নেয়। API 37-এর ফোনে PO-এর বিনিময় হার টাইপ করার কীবোর্ড খোলা অবস্থায় নিচের কাজের বোতাম সম্পূর্ণ দৃশ্যমান হয়েছে।

## মডিউল পর্যালোচনা

| মডিউল | সোর্স পর্যালোচনা | ডিভাইসে যাচাইয়ের বাকি অংশ |
| --- | --- | --- |
| Purchase ও receiving | সম্পাদনা পুনরুদ্ধার, রিসিভিং প্রস্তুতি, transaction, shortage এবং payable completion | প্যাকিং ও রিসিভিংয়ের বাস্তব পূর্ণ workflow |
| Product ও Stock | InventoryRepositoryImpl-এ adjustment, dispatch, receive ও cancel transaction-এর ভেতরে | FIFO ফল ও layer movement, barcode, ফিজিক্যাল ট্রান্সফার |
| Sales ও Contacts | SalesRepositoryImpl-এ invoice create-and-post, return, payment এবং delivery transaction-এর ভেতরে। কিছু Sales dark surfaces সংশোধিত | ডেলিভারি, রিটার্ন, কালেকশন, contact add/edit এবং ledger |
| Accounts ও Reports | Expense, reversal ও transfer-এর checkpoint transaction পর্যালোচিত | SQLite rollback, journal totals, report ও export |
| Settings ও Backup | Restore ফাইল যাচাই, লেখা এবং ওপেন Room-এর lifecycle পর্যালোচিত | বিচ্ছিন্ন ফিক্সচারে restore ও restart পরীক্ষা, security/settings আচরণ |

## খোলা ডেটা-নিরাপত্তার বিষয়

- BackupRepositoryImpl.restoreBackup সক্রিয় Room-এর SQLite ফাইল সরাসরি প্রতিস্থাপন করে এবং WAL/SHM সরিয়ে একই DAO দিয়ে audit লেখে। মিডিয়া কপি মাঝপথে ব্যর্থ হলে ডেটাবেস ও মিডিয়ার সমন্বিত রোলব্যাক নেই। আলাদা Termux SQLite ফিক্সচারে version 99-এর অসংশ্লিষ্ট ফাইল quick_check পাস করেছে। ওপেন সংযোগ পুরোনো order পড়েছে, যদিও ডিস্কে প্রতিস্থাপিত ফাইলে শুধু অসংশ্লিষ্ট টেবিল ছিল। নিরাপদ restore-এ Room schema যাচাই, সম্পূর্ণ প্যাকেজ staging এবং পরের cold start-এ Room খোলার আগে rollback-সহ প্রয়োগ দরকার।
- Startup seeding থেকে সব ব্যবসার সারি মুছে দেওয়া পুরোনো one-time cleanup সরানো হয়েছে। পুরোনো cleanup marker ছাড়াই PO, পণ্য, সরবরাহকারী ও গুদাম অক্ষত থাকে কি না, তার SQLite টেস্ট যোগ হয়েছে। ভবিষ্যৎ স্কিমার ডেটাবেস খুললে Room-এর ধ্বংসাত্মক downgrade fallback-ও সরানো হয়েছে।
- ডিভাইসে পরীক্ষা করতে আলাদা com.pocketdev.advance_erp2.premiumqa প্যাকেজ নির্ধারণ করা হয়েছে। লাইভ ERP-তে restore পরীক্ষা করা হয়নি।

## যাচাইয়ের ফল

- API 35 ও SHA-256 যাচাইকৃত Gradle 8.13 দিয়ে মূল debug বিল্ড পাস করেছে। সর্বশেষ সংশোধনসহ QA build-এ debug APK, ৩৫৮টি JVM টেস্ট এবং instrumentation APK পাস। প্রজেক্টের পিন করা ডিপেন্ডেন্সি একই আছে। [বিল্ড লগ](resume-build.log)
- API 37-এর ফোনে আলাদা QA প্যাকেজে ৩৯টি instrumentation টেস্ট পাস করেছে। এতে Compose PO খসড়া পুনরুদ্ধার, payable রেখে Purchase completion, concurrent completion, রিসিভিং ও expense rollback, startup seeding-এ ডেটা সুরক্ষা এবং real SQLite ledger টেস্ট রয়েছে। প্রথম visual run-এ Android dialog-এর surface সরে যাওয়ায় একটি capture ব্যর্থ হয়েছিল। ক্যাপচার এখন attached এবং visible window-এর জন্য অপেক্ষা করে। পরের পূর্ণ রান পাস করেছে। [ডিভাইস টেস্ট লগ](resume-device-tests.log)
- ৪৩টি যাচাইয়ের ছবি সংরক্ষিত হয়েছে। এর মধ্যে বাংলা, ইংরেজি, Light, Dark, বড় ফন্ট, কম প্রস্থ এবং বাস্তব numeric keyboard খোলা PO সম্পাদক আছে। আলাদা QA APK-এর SHA-256 হলো 9533a1a47ea6eb7ca6662153bf0fcdfc3cb60667099a14c6923d5de625db7f1f।
- আলাদা QA অ্যাপে Home, Purchase, Stock, Sales ও More এবং More থেকে Accounts, Products, Customers, Suppliers, Ledger ও Settings-এর স্ক্রিন খোলা হয়েছে। Reports-এর টপ বার দেখা গেছে। রিপোর্ট তৈরি ও এক্সপোর্টের পূর্ণ প্রবাহ যাচাই বাকি। More অন্য মডিউল থেকে ফিরলে সর্বশেষ সাবস্ক্রিন দেখাতে পারে। Android Back থেকে More হাব ফেরত আসে।
- সোর্সের git diff --check পাস। নিরাপদ staged backup restore ও সব মডিউলের শুরু থেকে শেষ পর্যন্ত ডিভাইস যাচাই খোলা আছে।
