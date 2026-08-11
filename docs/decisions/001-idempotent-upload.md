# 001 — Idempotent photo upload

## Context

The Python uploader processes batches of up to 15,000 CR3 files. To survive interruptions
(power outage, network failure, overheating), the uploader uses a JSONL checkpoint file
that records each successful upload. On restart it skips already-uploaded files.

The gap: if the server receives a file and responds 200, but the process dies before the
checkpoint is written, the same file gets uploaded again on resume — creating a duplicate
in R2 and in the database.

## Decision

`POST /fotos/upload` is idempotent by `code` (filename stem, uppercased).

Before processing, `FotoService` calls `fotoRepository.findByCode(code)`. If a record
already exists it is returned immediately — no watermark, no R2 upload, no DB insert.

## Consequences

- Duplicate uploads from the Python uploader are safe at any volume.
- A re-upload of a legitimately different photo with the same filename is silently ignored.
  This is acceptable: Canon sequential filenames (2V0Axxxx) are unique within a shooting day,
  which is the unit of work for one upload session.
- `FotoRepository` gains `findByCode(String)` — a simple derived query, no custom SQL needed.
