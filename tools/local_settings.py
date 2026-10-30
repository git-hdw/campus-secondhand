"""Resolve the Spring local database settings without exposing credentials."""
import os
from pathlib import Path
import re


def load_settings():
    backend = Path(__file__).resolve().parents[1] / 'afterend/campussecondhand'
    values = {}
    for path in (backend / 'src/main/resources/application.properties',
                 backend / '.local/runtime.properties'):
        if path.exists():
            for line in path.read_text(encoding='utf-8-sig').splitlines():
                if '=' in line and not line.lstrip().startswith('#'):
                    key, value = line.split('=', 1)
                    values[key.strip()] = value.strip()

    def resolve(match):
        name, separator, default = match.group(1).partition(':')
        value = os.environ.get(name, values.get(name, default if separator else None))
        if value is None:
            raise RuntimeError(f'Missing local configuration: {name}')
        return value

    return {key: re.sub(r'\$\{([^}]+)\}', resolve, value)
            for key, value in values.items() if key.startswith('spring.datasource.')}
