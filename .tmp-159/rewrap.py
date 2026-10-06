import re, sys, textwrap
def fix(path, broken):
    lines = open(path).read().split('\n')
    # merge broken continuation lines (1-based numbers) into the previous /// line
    for n in sorted(broken, reverse=True):
        i = n - 1
        assert lines[i-1].lstrip().startswith('///'), (path, n, lines[i-1])
        lines[i-1] = lines[i-1] + ' ' + lines[i].strip()
        del lines[i]
    # rewrap every /// block that has a line longer than its limit
    out = []
    i = 0
    while i < len(lines):
        m = re.match(r'^( *)///', lines[i])
        if not m:
            out.append(lines[i]); i += 1; continue
        ind = m.group(1)
        j = i
        while j < len(lines) and re.match(r'^' + ind + r'///', lines[j]):
            j += 1
        block = lines[i:j]
        limit = 120 - len(ind)
        if any(len(l) > limit for l in block):
            paras, cur = [], []
            for l in block:
                t = l[len(ind) + 3:].strip()
                if t == '':
                    paras.append(cur); cur = []
                    paras.append(None)
                else:
                    cur.append(t)
            paras.append(cur)
            for p in paras:
                if p is None:
                    out.append(ind + '///')
                elif p:
                    for w in textwrap.wrap(' '.join(p), width=limit - len(ind) - 4, break_long_words=False, break_on_hyphens=False):
                        out.append(ind + '/// ' + w)
        else:
            out.extend(block)
        i = j
    open(path, 'w').write('\n'.join(out))
base='zazr-core/src/main/java/dev/zazr/collection/internal/'
fix(base+'Arrangements.java', [59,62,66,68])
fix(base+'BitmapIndexedSetNode.java', [749,751])
fix(base+'LazyListModule.java', [246,248])
fix(base+'ListModule.java', [64])
