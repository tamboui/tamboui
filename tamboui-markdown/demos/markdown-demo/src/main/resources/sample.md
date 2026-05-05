# TamboUI Markdown

Welcome to the **markdown renderer** demo. This view supports a useful subset
of CommonMark and the *most-used* GFM extensions.

## Inline styling

You can use **bold**, *italic*, ~~strikethrough~~, `inline code`, and
[hyperlinks](https://github.com/tamboui/tamboui).

## Lists

Bullet lists:

- buffers are diffed before flush
- widgets render to a single `Buffer`
- the toolkit is a thin DSL on top

Ordered lists:

1. parse markdown
2. layout into chunks
3. render to the buffer

GFM task lists:

- [x] parse CommonMark
- [x] handle GFM tables
- [ ] add toolkit DSL element

## Tables

| Widget    | Stateful | Module          |
|-----------|----------|-----------------|
| Paragraph | no       | tamboui-widgets |
| Table     | yes      | tamboui-widgets |
| Markdown  | no       | tamboui-markdown|

## Code

Fenced code blocks keep their info string as a title:

```java
MarkdownView view = MarkdownView.builder()
    .source(text)
    .build();
view.render(area, buffer);
```

## Quote

> The trick is not how to render markdown,
> but how to render it well when the input is incomplete.

---

Press **j**/**k** to scroll, **s** to toggle the streaming simulation, and
**q** to quit.
