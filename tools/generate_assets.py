import argparse
import json
from pathlib import Path


def _read_chars(path: Path) -> list[str]:
    text = path.read_text(encoding="utf-8")
    chars: list[str] = []
    for raw in text.splitlines():
        line = raw.strip()
        if not line:
            continue
        chars.append(line[0])
    seen: set[str] = set()
    deduped: list[str] = []
    for ch in chars:
        if ch in seen:
            continue
        seen.add(ch)
        deduped.append(ch)
    return deduped


def _codepoint_file_name(ch: str) -> str:
    cp = ord(ch)
    return f"u{cp:x}.json"


def _validate_phrases(phrases: dict[str, list[str]]) -> None:
    for ch, items in phrases.items():
        if not isinstance(items, list):
            raise ValueError(f"phrases[{ch}] must be a list")
        for phrase in items:
            if len(phrase) > 5:
                raise ValueError(f"phrase too long (>{5}): {ch} -> {phrase}")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--graphics", type=Path, required=True)
    parser.add_argument("--dictionary", type=Path, required=True)
    parser.add_argument("--lesson", type=Path, required=True)
    parser.add_argument("--phrases", type=Path, required=True)
    parser.add_argument("--out-assets", type=Path, required=True)
    args = parser.parse_args()

    lesson_chars = _read_chars(args.lesson)
    lesson_set = set(lesson_chars)

    phrases = json.loads(args.phrases.read_text(encoding="utf-8"))
    _validate_phrases(phrases)

    out_assets: Path = args.out_assets
    out_assets.mkdir(parents=True, exist_ok=True)
    out_char_dir = out_assets / "char_data"
    out_char_dir.mkdir(parents=True, exist_ok=True)

    pinyin_by_char: dict[str, list[str]] = {}
    with args.dictionary.open("r", encoding="utf-8") as f:
        for line in f:
            raw = line.strip()
            if not raw:
                continue
            obj = json.loads(raw)
            ch = obj.get("character")
            if ch not in lesson_set:
                continue
            pinyin_by_char[ch] = obj.get("pinyin") or []

    found: set[str] = set()
    stroke_count_by_char: dict[str, int] = {}
    with args.graphics.open("r", encoding="utf-8") as f:
        for line in f:
            raw = line.strip()
            if not raw:
                continue
            obj = json.loads(raw)
            ch = obj.get("character")
            if ch not in lesson_set:
                continue
            strokes = obj.get("strokes")
            medians = obj.get("medians")
            if not isinstance(strokes, list) or not isinstance(medians, list):
                continue
            out_obj = {
                "strokes": strokes,
                "medians": medians,
            }
            out_file = out_char_dir / _codepoint_file_name(ch)
            out_file.write_text(json.dumps(out_obj, ensure_ascii=False), encoding="utf-8")
            found.add(ch)
            stroke_count_by_char[ch] = len(strokes)

    missing = [ch for ch in lesson_chars if ch not in found]
    if missing:
        raise RuntimeError(f"missing char data: {''.join(missing)}")

    index_items: list[dict] = []
    for ch in lesson_chars:
        index_items.append(
            {
                "char": ch,
                "codepoint": ord(ch),
                "file": f"char_data/{_codepoint_file_name(ch)}",
                "pinyin": pinyin_by_char.get(ch, []),
                "strokeCount": stroke_count_by_char.get(ch, 0),
                "phrases": phrases.get(ch, []),
            }
        )
    index_items.sort(key=lambda x: x["codepoint"])

    (out_assets / "char_index.json").write_text(
        json.dumps(index_items, ensure_ascii=False, separators=(",", ":")),
        encoding="utf-8",
    )


if __name__ == "__main__":
    main()
