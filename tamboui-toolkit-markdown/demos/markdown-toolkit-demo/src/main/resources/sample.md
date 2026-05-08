# MarkdownElement (Toolkit)

This is the **toolkit-level** wrapper for `MarkdownView`. It picks up CSS
through the same engine the rest of the toolkit uses, so element-level
*selectors* drive the style.

## Inline

You can use **bold**, *italic*, ~~strikethrough~~, `inline code`, and
[hyperlinks](https://example.com).

## Lists

- buffers are diffed before flush
- widgets render to a single `Buffer`
- the toolkit is a thin DSL on top

GFM task lists:

- [x] parse CommonMark
- [x] handle GFM tables
- [ ] add toolkit DSL element

## Tables

| Widget    | Stateful | Module               |
|-----------|----------|----------------------|
| Paragraph | no       | tamboui-widgets      |
| Markdown  | no       | tamboui-markdown     |
| Markdown  | no       | tamboui-toolkit-markdown |

## Code

```java
panel("README",
    markdown(readme).fill()
).rounded();
```

## Quote

> CSS for the toolkit element flows through to the underlying widget.

---

Press **↑**/**↓** to scroll, **t** to cycle the theme, **q** to quit.
