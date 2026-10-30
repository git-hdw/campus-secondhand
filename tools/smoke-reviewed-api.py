"""Read-only runtime checks. Credentials come from REVIEW_* environment variables."""
import json
import os
from pathlib import Path
from urllib.error import HTTPError
from urllib.request import Request, urlopen

BASE = 'http://127.0.0.1:8080/api'
results = []


def call(method, path, body=None, token=None):
    headers = {'Content-Type': 'application/json'}
    if token:
        headers['Authorization'] = 'Bearer ' + token
    request = Request(BASE + path, method=method, headers=headers,
                      data=None if body is None else json.dumps(body).encode())
    try:
        with urlopen(request, timeout=15) as response:
            return response.status, json.load(response)
    except HTTPError as error:
        return error.code, json.load(error)


def check(name, response, expected=200):
    status, body = response
    passed = status == expected and body.get('code') == expected
    results.append({'name': name, 'passed': passed, 'status': status})
    print(('PASS ' if passed else 'FAIL ') + name)
    if not passed:
        raise AssertionError(f'{name}: unexpected HTTP/business status')
    return body.get('data')


def login(prefix):
    data = check(prefix + ' login', call('POST', '/auth/login', {
        'username': os.environ[prefix + '_USERNAME'],
        'password': os.environ[prefix + '_PASSWORD']}))
    return data['token']


try:
    check('public statistics', call('GET', '/stats'))
    listing = check('public paginated products', call('GET', '/products/list?page=1&pageSize=8'))
    assert len(listing['items']) <= 8
    check('public categories', call('GET', '/categories'))
    check('missing product', call('GET', '/products/9223372036854775807'), 404)
    check('invalid price range', call('GET', '/products/list?minPrice=100&maxPrice=10'), 400)
    check('missing search parameter', call('GET', '/products/search'), 400)
    check('anonymous private endpoint', call('GET', '/orders/my'), 401)
    check('anonymous administration', call('GET', '/admin/users'), 401)
    check('invalid session', call('GET', '/user/profile', token='invalid'), 401)
    check('empty login', call('POST', '/auth/login', {}), 400)
    if os.environ.get('REVIEW_USERNAME'):
        token = login('REVIEW')
        for path in ['/user/profile', '/products/my', '/orders/my', '/favorites/my',
                     '/messages/received', '/messages/sent', '/messages/unread-count']:
            check('student ' + path, call('GET', path, token=token))
        check('student denied administration', call('GET', '/admin/users', token=token), 403)
        check('missing private order', call('GET', '/orders/9223372036854775807', token=token), 404)
        for path in ['/products/my?page=1&pageSize=2', '/orders/my?page=1&pageSize=2', '/favorites/my?page=1&pageSize=2', '/messages/conversations?page=1&pageSize=2']:
            page = check('bounded ' + path, call('GET', path, token=token))
            assert len(page['items']) <= 2 and 'total' in page
        check('session list', call('GET', '/auth/sessions', token=token))
        check('revoke current session', call('POST', '/auth/logout', token=token))
        check('revoked session rejected', call('GET', '/user/profile', token=token), 401)
    if os.environ.get('REVIEW_ADMIN_USERNAME'):
        token = login('REVIEW_ADMIN')
        for path in ['/stats', '/users', '/products', '/orders', '/messages', '/categories']:
            check('admin ' + path, call('GET', '/admin' + path, token=token))
    with urlopen('http://localhost:5173/', timeout=10) as response:
        assert response.status == 200 and b'id="app"' in response.read()
    results.append({'name': 'frontend entry', 'passed': True, 'status': 200})
    with urlopen('http://localhost:5173/api/stats', timeout=10) as response:
        assert response.status == 200 and json.load(response)['code'] == 200
    results.append({'name': 'frontend API proxy', 'passed': True, 'status': 200})
finally:
    output = Path(__file__).resolve().parents[1] / '.review/api-smoke-results.json'
    output.parent.mkdir(exist_ok=True)
    output.write_text(json.dumps(results, ensure_ascii=False, indent=2), encoding='utf-8')
print(f'{len(results)} runtime checks passed')
