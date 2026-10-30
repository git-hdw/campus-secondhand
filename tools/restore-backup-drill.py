"""Restore a fresh backup into an isolated DB and compare business-table checksums."""
import os, re, subprocess, uuid
from pathlib import Path
from local_settings import load_settings
root = Path(__file__).resolve().parents[1]
settings = load_settings()
env = os.environ.copy(); env['MYSQL_PWD'] = settings['spring.datasource.password']
cli = [r'D:\phpstudy_pro\Extensions\MySQL8.0.12\bin\mysql.exe', '-u' + settings['spring.datasource.username'], '--default-character-set=utf8mb4', '-N', '-B']
def sql(query, database=None):
    return subprocess.run(cli + ([database] if database else []), input=query, env=env, text=True, encoding='utf-8', capture_output=True, check=True).stdout.strip()
subprocess.run(['python', str(root / 'tools/backup-before-relations.py')], check=True)
backup = max((root / '.review/db-backups').glob('before-relations-*.sql'), key=lambda p:p.stat().st_mtime)
content = backup.read_text(encoding='utf-8')
assert not re.search(r'(?im)^\s*(USE\s|CREATE\s+DATABASE)', content), 'Dump must not redirect to another database'
database = 'campus_restore_' + uuid.uuid4().hex
assert re.fullmatch(r'campus_restore_[a-f0-9]{32}', database)
sql(f'CREATE DATABASE `{database}` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci')
try:
    sql(content, database)
    for table in ['users','categories','products','orders','favorites','messages']:
        live = sql(f'SELECT COUNT(*) FROM `{table}`; CHECKSUM TABLE `{table}`', 'campus_secondhand').splitlines()
        restored = sql(f'SELECT COUNT(*) FROM `{table}`; CHECKSUM TABLE `{table}`', database).splitlines()
        assert live[0] == restored[0] and live[1].split('\t')[-1] == restored[1].split('\t')[-1], table + ' mismatch'
        print('PASS restore count and checksum:', table, restored[0])
    print('Restore drill passed; original database was not modified')
finally:
    assert re.fullmatch(r'campus_restore_[a-f0-9]{32}', database)
    sql(f'DROP DATABASE `{database}`')
