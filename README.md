# Cython-Fix

A PyCharm plugin that fixes parser and formatter bugs with Cython files.

## Problems Fixed

### Parser
- **False "duplicate parameter name" error** on `ndim=1` in NumPy buffer syntax like `np.ndarray[np.float64_t, ndim=1]`

### Formatter
- **Double pointer declarations get unwanted spaces:** `float **x` becomes `float ** x`

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

The plugin registers replacement implementations for Cython's parser and formatter that take precedence over the built-in ones:

- **Parser:** Fixes the argument parsing logic for buffer syntax to not treat `ndim` as a duplicate parameter
- **Formatter:** Uses `SpacingBuilder` rules to prevent spaces in casts and address-of, plus a `Block` wrapper for pointer declarations in type contexts

## License

MIT
