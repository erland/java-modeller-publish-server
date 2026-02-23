# Third-Party Notices

This project is licensed under the MIT License (see `LICENSE`). It depends on third‑party components that are distributed under their own licenses. When redistributing this project (source or binaries), you must retain the applicable license texts/notices for these components.

## Notable dependencies and their licenses

### Apache License 2.0
- **Quarkus** (`io.quarkus:*`) — Quarkus is licensed under Apache License 2.0.  
  Source / license: https://github.com/quarkusio/quarkus/blob/main/LICENSE  (Apache-2.0)
- **REST Assured** (`io.rest-assured:rest-assured`, test scope) — Apache License 2.0.  
  Source / license: https://github.com/rest-assured/rest-assured/blob/master/LICENSE  (Apache-2.0)

### Eclipse Public License 2.0 (often dual with GPLv2 + Classpath Exception)
- **Jakarta JSON Processing API** (`jakarta.json:jakarta.json-api`) — typically EPL-2.0 (and in some distributions dual-licensed with GPLv2 + Classpath Exception).  
  Project info: https://github.com/jakartaee/jsonp-api
- **Eclipse Parsson** (`org.eclipse.parsson:parsson`) — EPL-2.0 (and may list GPLv2 + Classpath Exception as a secondary license).  
  Project / license: https://github.com/eclipse-ee4j/parsson/blob/master/LICENSE.md

## Practical implications

- Your repository’s **MIT** license applies to *your* original code.
- Third‑party libraries remain under their own licenses (e.g., Apache‑2.0, EPL‑2.0).
- You generally comply by:
  - Shipping your `LICENSE`,
  - Keeping this `THIRD_PARTY_NOTICES.md` (or equivalent),
  - Preserving any upstream NOTICE files (where applicable).

If you modify and redistribute EPL‑licensed components themselves, EPL obligations may require making those modifications available under EPL terms.
