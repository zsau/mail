# mail

Extremely basic Clojure wrapper for `javax.mail`. Only supports uploading messages at the moment!

## Artifact

Leiningen dependency:
```clojure
[zsau/mail "0.1.0"]
```

## Usage

```clojure
(def imap {:host "imap.example.com"
           :port 993 ; optional (default: protocol-dependent)
           :user "nobody"
           :password "hunter2"
           :protocol "imaps"})] ; optional (default: "imaps")

(def folder {:name "Misc"
             :create true}) ; optional (default: false)

(def message {:from "jsmith@example.com" ; alternatively: (address "jsmith@example.com" "John Smith")
              :to "nobody@example.com"
              :subject "Hello"
              :body "Hello, world!"
              :date (java.util.Date.)}) ; optional (default: now)

(with-store [store imap]
  (append-messages store folder [message ...])))
```
## License

Released under the [MIT License](https://opensource.org/licenses/MIT).
