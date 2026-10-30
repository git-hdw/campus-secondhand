"""Create a full local backup before the relation migration; never prints credentials."""
from pathlib import Path
from datetime import datetime
import os, subprocess, hashlib
from local_settings import load_settings
root = Path(__file__).resolve().parents[1]
folder = root / '.review/db-backups'
folder.mkdir(parents=True, exist_ok=True)
output = folder / ('before-relations-' + datetime.now().strftime('%Y%m%d-%H%M%S') + '.sql')
settings = load_settings()
env = os.environ.copy()
env['MYSQL_PWD'] = settings['spring.datasource.password']
subprocess.run([r'D:\phpstudy_pro\Extensions\MySQL8.0.12\bin\mysqldump.exe', '-u' + settings['spring.datasource.username'],
                '--lock-all-tables', '--default-character-set=utf8mb4', '--set-gtid-purged=OFF',
                '--result-file=' + str(output), 'campus_secondhand'], env=env, check=True, capture_output=True)
assert output.stat().st_size > 0
print('Backup:', output)
print('SHA256:', hashlib.sha256(output.read_bytes()).hexdigest())
