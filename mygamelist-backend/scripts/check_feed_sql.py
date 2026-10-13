"""Check native feed SQL semantics with SQLite; does not replace a PostgreSQL/JPA integration test."""
from pathlib import Path
import re
import sqlite3
source = (Path(__file__).resolve().parents[1] / 'src/main/java/com/caiotcruz/mygamelist/repository/ActivityRepository.java').read_text()
queries = re.findall(r'@Query\(value = """(.*?)""", nativeQuery = true\)', source, re.S)
assert len(queries) == 2
conn = sqlite3.connect(':memory:')
conn.row_factory = sqlite3.Row
conn.executescript('''CREATE TABLE tb_activities (id INTEGER PRIMARY KEY, group_id TEXT, user_id INTEGER);
CREATE TABLE tb_follows (follower_id INTEGER, followed_id INTEGER);
INSERT INTO tb_follows VALUES (1, 2);
''')
# Each complete group has four events. Include a legacy ungrouped event and private-to-this-feed author.
for group in range(25):
    for part in range(4):
        conn.execute('INSERT INTO tb_activities VALUES (?,?,?)', (group * 4 + part + 1, f'g{group}', 2))
conn.execute('INSERT INTO tb_activities VALUES (101,NULL,1)')
conn.execute('INSERT INTO tb_activities VALUES (102,\'hidden\',3)')
first = conn.execute(queries[0], {'userId': 1, 'beforeId': 2**63-1, 'limit': 21}).fetchall()
assert len(first) == 21 and first[0]['groupKey'] == 'legacy-101'
assert all(g['groupKey'] != 'hidden' for g in first)
selected = first[:20]
keys = [g['groupKey'] for g in selected]
load_sql = queries[1].replace('IN (:keys)', 'IN (' + ','.join(f':key{i}' for i in range(len(keys))) + ')')
params = {'userId':1, **{f'key{i}': key for i,key in enumerate(keys)}}
events = conn.execute(load_sql, params).fetchall()
assert len(events) == 1 + 19 * 4, 'A page must contain all events of every selected group'
conn.execute('INSERT INTO tb_activities VALUES (999,\'new\',2)')
second = conn.execute(queries[0], {'userId':1, 'beforeId':selected[-1]['anchorId'], 'limit':21}).fetchall()
assert len(second) == 6
assert set(keys).isdisjoint(g['groupKey'] for g in second)
assert all(g['groupKey'] not in ('hidden','new') for g in second)
assert len(keys) + len(second) == 26
print('SQL do feed: grupos completos, legado, visibilidade e cursor estável passaram (SQLite).')
