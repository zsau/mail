(ns mail)

(def ^:private service-session-field
	(doto (.getDeclaredField (Class/forName "javax.mail.Service") "session")
		(.setAccessible true)))

(defn ^:private session
	"Creates a Session using the given protocol name (e.g. 'imaps')."
	[protocol]
	(javax.mail.Session/getInstance
		(doto (java.util.Properties.)
			(.setProperty "mail.store.protocol" protocol)
			(.setProperty (str "mail." protocol ".ssl.enable") "true")
			(.setProperty (str "mail." protocol ".ssl.checkserveridentity") "true"))))

(defn ^:private connect-store!
	"Connects the given Store. Options include :user, :password, :host, and :port (optional)."
	[store {:keys [host port user password]}]
	(if port
		(.connect store host port user password)
		(.connect store host user password)))

(defn ^:private stream->message
	"Deserializes a `MimeMessage` from the given input stream."
	[session istream]
	(javax.mail.internet.MimeMessage. session istream))

(defn ^:private map->message
	"Creates a `MimeMessage` using :from, :to, :date, :subject, and :body. :from and :to can be strings or `InternetAddress` objects (see `address`) and :date is an optional `java.util.Date` (default: now)."
	[session {:keys [from to date subject body]}]
	(doto (javax.mail.internet.MimeMessage. session)
		(.setText body "UTF-8" "html")
		(.setSubject subject)
		(.setSentDate (or date (java.util.Date.)))
		(.setFrom from)
		(.addRecipient javax.mail.Message$RecipientType/TO to)))

(defn ^:private to-message [session stream-or-map]
	(if (map? stream-or-map)
		(map->message session stream-or-map)
		(stream->message stream-or-map)))

(defn with-folder*
	"Opens a folder, evaluates `(f folder)`, then closes the folder. Options:
  :name    name of the folder (can include slashes)
  :create  create folder if nonexistent (optional, default false)"
	[store {:keys [name create]} f]
	(let [
			folder (.getFolder store name)
			ensure-exists (fn [folder]
				(when (and create (not (.exists folder)))
					(.create folder javax.mail.Folder/HOLDS_MESSAGES)))]
		(try
			(f (doto folder
				(ensure-exists)
				(.open javax.mail.Folder/READ_WRITE)))
			(finally (when (.isOpen folder) (.close folder true))))))

(defmacro with-folder
	"Evaluates `body` with `folder-sym` bound to an open folder. See with-folder*."
	[[folder-sym store opts] & body]
	`(with-folder* ~store ~opts
		(fn [~folder-sym] ~@body)))

(defn append-messages
	"Appends the given list of messages to `store`, in the folder specified by `folder-opts` (see `with-folder*`). Messages can be maps (see `map->message`) or input streams."
	[store folder-opts messages]
	(when (seq messages)
		(with-folder [folder store folder-opts]
			(.appendMessages folder (into-array
				(map (partial to-message (.get service-session-field store))
					messages))))))

(defn with-store*
	"Connects to a Store with the given options, evaluates `(f store)`, then closes the connection. Options:
  :host      server hostname
  :port      server port (optional, default protocol-dependent)
  :user      account username
  :password  account password
  :protocol  protocol to connect via (optional, default 'imaps')"
	[config f]
	(let [session (session (:protocol config "imaps"))]
		(with-open [store (doto (.getStore session) (connect-store! config))]
			(f store))))

(defmacro with-store
	"Evaluates `body` with `store-sym` bound to a connected Store. See with-store*."
	[[store-sym config] & body]
	`(with-store* ~config
		(fn [~store-sym] ~@body)))

(defn address
	"Creates an InternetAddress with the given email address and optional personal name."
	([email] (address email nil))
	([email personal] (javax.mail.internet.InternetAddress. email personal)))
