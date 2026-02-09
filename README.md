# Cython-Fix

A PyCharm plugin that fixes parser and formatter bugs with Cython files.

## Problems Fixed

### Parser
- **False "duplicate parameter name" error** on `ndim=1` in NumPy buffer syntax like `np.ndarray[np.float64_t, ndim=1]`
- **False "duplicate parameter name" error** on unnamed parameters in `cdef extern` declarations like `void f(char *, char *)`

### Formatter
- **Double pointer declarations get unwanted spaces:** `float **x` becomes `float ** x`

### Type Resolution
- **"Unexpected parameter" on `cdef class` constructors** when the class uses `__cinit__` instead of `__init__` — PyCharm only looks for `__init__`/`__new__`, so constructor arguments are flagged as errors

### Syntax Highlighting
- **Docstrings in `cdef`/`cpdef` functions shown as plain strings** — PyCharm's lexer only recognizes docstrings after `def`/`class` keywords, so `cdef`/`cpdef` docstrings miss the docstring coloring

## Installation

1. Download the latest release ZIP from the releases page
2. In PyCharm: **Settings → Plugins → ⚙️ → Install Plugin from Disk...**
3. Select the ZIP file and restart PyCharm

## Building from Source

Requires JDK 21+.

```bash
./gradlew buildPlugin
```

The plugin ZIP will be in `build/distributions/`.

## Compatibility

- PyCharm Professional 2025.3+
- Requires the Cython language support (bundled with PyCharm Professional)

## How It Works

The plugin registers replacement implementations and extensions that take precedence over the built-in ones:

- **Parser:** Fixes the argument parsing logic for buffer syntax to not treat `ndim` as a duplicate parameter, and assigns unique synthetic names to unnamed parameters in extern declarations
- **Formatter:** Uses `SpacingBuilder` rules to prevent spaces in casts and address-of, plus a `Block` wrapper for pointer declarations in type contexts
- **Type provider:** Intercepts constructor call resolution for `cdef` classes and delegates parameter info to `__cinit__` when no `__init__` is defined
- **Annotator:** Applies docstring highlighting to the first string literal in `cdef`/`cpdef` function and class bodies

## License

MIT
