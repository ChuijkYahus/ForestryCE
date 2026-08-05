## Comment & Javadoc style (binding)

House style is **Oracle's *How to Write Doc Comments for the Javadoc Tool* + Google Java Style §7** for
structure, plus **ASD-STE100's Writing Rules** (the rules only, *not* its ~900-word approved dictionary,
which would reject `pollinate`, `karyotype`, etc.) for diction. Rules below are derived from measuring the
existing corpus; match it, don't improve on it.

**Javadoc**

- Wrap at 120 columns. Existing `api/` averages ~49 columns per line, so err short.
- `@return` and `@param` are **noun-phrase fragments with no terminal period**. `@param` descriptions start
  with `The` unless that is genuinely wrong. Keep `@return` to one line where possible.
- Method descriptions are third-person declarative: `Used to ...`, `Sets ...`, `Determines ...`,
  `Registers ...`, `Called when ...`, `Adds ...`. Reuse the **identical** opening verb across parallel
  members. Never vary a verb for rhythm or to avoid repetition. This is the single most important rule here.
- One idea per chunk. Separate chunks with a bare `*` line; use `<p>` only for a true second paragraph.
- Examples use the `Ex.` marker and nothing else: `Ex. "species_type.forestry.bee" -> "Bee"`,
  `(ex. decaying leaves)`.

**Inline comments**

- Fragments, terse. Median length in this repo is ~34 chars. State just enough, then stop.
- **No terminal period.** Add one only when the comment reaches two full sentences.
- Sentence case is free variation (the corpus runs ~42% lowercase / 58% capitalized). Do not normalize it,
  and do not spend effort deciding.
- Established domain acronyms (`NBT`, `JEI`, `API`, `GUI`, `ID`) go in bare and unexpanded. Never coin a new
  abbreviation.
- Lowercase `todo` is the dominant form (~260 vs ~18 `TODO`).

**Punctuation: ASCII only.** The existing corpus contains **zero** non-ASCII characters across 5,195
comment/Javadoc lines. No em-dash, no en-dash, no curly quotes, no `…`. The only dash is hyphen-minus, and
only as a minus sign or a list separator. Recast an em-dash parenthetical as a separate sentence, a comma
pair, or parentheses; never substitute ` - ` or ` -- ` for it.

**Avoid** (these read as machine-written): hedges (`it's worth noting`, `essentially`, `simply`), adverb
padding (`seamlessly`, `robustly`, `carefully`), restating the method signature in prose, and elegant
variation across sibling members.
