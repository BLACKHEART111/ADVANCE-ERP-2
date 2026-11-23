from contextlib import closing
import sqlite3
import unittest

from rehearse_history import date_ledger, stock_projection


class HistoryPolicyTests(unittest.TestCase):
    def fixture(self):
        db = sqlite3.connect(":memory:")
        db.row_factory = sqlite3.Row
        db.executescript("""
            CREATE TABLE customers(id TEXT, opening_balance INTEGER,total_due INTEGER);
            CREATE TABLE sales_orders(id TEXT,customer_id TEXT,orderDate INTEGER,order_number TEXT,total INTEGER,is_deleted INTEGER);
            CREATE TABLE payments(id TEXT,customer_id TEXT,paymentDate INTEGER,reference TEXT,amount INTEGER,is_deleted INTEGER);
            CREATE TABLE sales_returns(id TEXT,sale_order_id TEXT,return_date INTEGER,return_number TEXT,total INTEGER,is_deleted INTEGER);
            CREATE TABLE customer_ledger(id TEXT,customer_id TEXT,transactionDate INTEGER,notes TEXT,amount INTEGER,transaction_type TEXT,is_deleted INTEGER);
            INSERT INTO customers VALUES('c',0,-4);
            INSERT INTO sales_orders VALUES('i','c',1000,'INV',100,0);
            INSERT INTO payments VALUES('p','c',3000,'PAY',102,0);
            INSERT INTO customer_ledger VALUES('a','c',2000,'discount',-2,'ADJUSTMENT',0);
        """)
        return db

    def test_customer_credit_is_preserved_without_invoice_allocations(self):
        with closing(self.fixture()) as db:
            ledger = date_ledger(db)
            self.assertEqual([r["kind"] for r in ledger], ["INVOICE", "ADJUSTMENT", "PAYMENT"])
            self.assertEqual(ledger[-1]["running_balance_minor"], -400)
            self.assertTrue(all("invoice_id" not in r for r in ledger))

    def test_nonreconciling_ledger_is_rejected(self):
        with closing(self.fixture()) as db:
            db.execute("UPDATE customers SET total_due=99")
            with self.assertRaisesRegex(ValueError, "does not match"):
                date_ledger(db)

    def test_confirmed_zero_overrides_only_stale_variant_balances(self):
        with closing(sqlite3.connect(":memory:")) as db:
            db.row_factory=sqlite3.Row
            db.executescript("""
                CREATE TABLE products(id TEXT,name TEXT,current_stock INTEGER);
                CREATE TABLE product_variants(id TEXT,product_id TEXT,current_stock INTEGER,is_deleted INTEGER);
                CREATE TABLE batches(product_id TEXT,remaining_quantity INTEGER,is_deleted INTEGER);
                INSERT INTO products VALUES('p','SHOES INSOLE RETAIL ',0);
                INSERT INTO product_variants VALUES('v','p',240,0);
                INSERT INTO batches VALUES('p',0,0);
            """)
            result = stock_projection(db)[0]
            self.assertEqual(result["quantity"], 0)
            self.assertEqual(result["variant_quantities"], {"v":0})
            db.execute("UPDATE products SET current_stock=240")
            db.execute("UPDATE batches SET remaining_quantity=240")
            with self.assertRaises(ValueError):
                stock_projection(db)


if __name__ == "__main__":
    unittest.main()
