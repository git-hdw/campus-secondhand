"""Read-only relation audit against the local database; prints counts, never credentials."""
import os
import subprocess
from local_settings import load_settings

settings = load_settings()
env = os.environ.copy()
env['MYSQL_PWD'] = settings['spring.datasource.password']
relations = [('products', 'user_id', 'users', 'id'), ('products', 'category', 'categories', 'name'),
             ('orders', 'buyer_id', 'users', 'id'), ('orders', 'seller_id', 'users', 'id'),
             ('orders', 'product_id', 'products', 'id'), ('favorites', 'user_id', 'users', 'id'),
             ('favorites', 'product_id', 'products', 'id'), ('messages', 'sender_id', 'users', 'id'),
             ('messages', 'receiver_id', 'users', 'id'), ('messages', 'product_id', 'products', 'id'),
             ('categories', 'parent_id', 'categories', 'id'), ('request_dedup', 'user_id', 'users', 'id')]
queries = [f"SELECT '{table}.{field}',COUNT(*) FROM `{table}` c LEFT JOIN `{parent}` p ON c.`{field}`=p.`{key}` WHERE c.`{field}` IS NOT NULL AND p.`{key}` IS NULL" for table, field, parent, key in relations]
queries += ["SELECT 'duplicate pending',COUNT(*) FROM (SELECT product_id FROM orders WHERE status=0 AND product_id IS NOT NULL GROUP BY product_id HAVING COUNT(*)>1) t",
            "SELECT 'duplicate completed',COUNT(*) FROM (SELECT product_id FROM orders WHERE status=3 AND product_id IS NOT NULL GROUP BY product_id HAVING COUNT(*)>1) t"]
queries += ["SELECT 'preserved legacy orders',COUNT(*) FROM orders WHERE product_id IS NULL AND legacy_product_id IS NOT NULL",
            "SELECT 'foreign keys',COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE CONSTRAINT_SCHEMA='campus_secondhand' AND CONSTRAINT_TYPE='FOREIGN KEY'",
            "SELECT 'migration version',MAX(version) FROM schema_migrations"]
queries += ["SELECT TABLE_NAME,ENGINE,TABLE_COLLATION FROM information_schema.TABLES WHERE TABLE_SCHEMA='campus_secondhand'",
            "SELECT TABLE_NAME,COLUMN_NAME,COLUMN_TYPE,COLLATION_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='campus_secondhand' AND COLUMN_NAME IN ('id','user_id','category','name')"]
result = subprocess.run([r'D:\phpstudy_pro\Extensions\MySQL8.0.12\bin\mysql.exe', '-u' + settings['spring.datasource.username'],
                         '--default-character-set=utf8mb4', '-N', '-B', 'campus_secondhand'],
                        input=';\n'.join(queries) + ';', env=env, text=True, capture_output=True, check=True)
print(result.stdout)
