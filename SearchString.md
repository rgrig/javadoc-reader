# Definition #

Everything here is case insensitive, for now.

When the search box contains `S` the class list and the package list contain `search(S)`, where:

```
def reallySearch(R):
  rc, rp = [], []
  for c in classes: if qualifiedName(c).matches(R): rc.append(c)
  for p in packages: if name(p).matches(R): rp.append(p)
  return (rc, rp)

def search(S):
  foreach searchType:
    if searchType.C(S): return reallySearch(searchType.R(S))
```

The search types are tried in the following order:
| **search type** | **C(S)** | **R(S)** |
|:----------------|:---------|:---------|
| regex           | regex checkbox is checked | S        |
| substring       | `forall c in S: isIdChar(c)` | `".*" + S + "[^\.]*"` |
| qualified       | `forall c in S: isIdChar(c) or c == '.'` | `".*" + S["."->"\."] + ".*"` |
| wildcard        | `otherwise` | `S["."->"\."]["*"->".*"]` |

Notations:
  * `S[A->B]` is the string obtained by replacing all occurrences of the substring `A` in `S` by `B`. For example `"java.util*"["."->"\."]["*"->".*"]` stands for `"java\.util.*"`. If there are overlapping substrings in `S` that equal `A` then the notation is undefined.
  * "\" stands for a string that contains one character, that is, we don't use backslash for escaping in strings, although it still escapes in regular expressions
  * `isIdChar(c)` is short for `Character.isJavaIdentifierPart(c)`
  * `otherwise` is long for `true` :)

Comment:
  * the `String.matches()` function takes a javascript regular expression as an argument in GWT

# Examples #

The table collects mostly weird cases:
| **S** | **type** | **R(S)** | **comment** |
|:------|:---------|:---------|:------------|
| `"util"` | substring | `".*util[^\.]*"` | the classes in `java.util` won't match, but `javax.swing.SwingUtilities` will; the _package_ `java.util` will match |
| `"map"` | substring | `".*map[^\.]*"` | `java.util.Map.Entry` does _not_ match, but `java.util.Map` does; this may be surprising |
| `"util.map"` | qualified | `".*util\.map.*"` | both `java.util.Map.Entry` and `java.util.Map` match |
| `"java"` | substring | `".*java[^\.]*"` | the packages starting with `java.` will _not_ match |
| `"java."` | qualified | `".*java\..*"` | the packagess starting with `java.` _will_ match |
| `"*exception"` | wildcard | `".*exception"` | all class names ending in `"Exception"` match |
| `"java.*format"` | wildcard | `"java\..*format"` | finds `java.text.NumberFormat` but not `java.text.NumberFormat.Field` |

Comment:
  * The dot has three functions in Java identifiers: (1) separates packages, (2) separates nested classes, (3) separates a package from a class. This is related to the "surprising behavior" when the search string is `"map"`.

# Implementation #

The implementation does _not_ have to follow the algorithm above as long as the results are the same. For example, the substring search works with unqualified class names too.