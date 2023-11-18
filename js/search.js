// When the user clicks on the search box, we want to toggle the search dropdown
function displayToggleSearch(e) {
  e.preventDefault();
  e.stopPropagation();

  closeDropdownSearch(e);
  
  if (idx === null) {
    console.log("Building search index...");
    prepareIdxAndDocMap();
    console.log("Search index built.");
  }
  const dropdown = document.querySelector("#search-dropdown-content");
  if (dropdown) {
    if (!dropdown.classList.contains("show")) {
      dropdown.classList.add("show");
    }
    document.addEventListener("click", closeDropdownSearch);
    document.addEventListener("keydown", searchOnKeyDown);
    document.addEventListener("keyup", searchOnKeyUp);
  }
}

//We want to prepare the index only after clicking the search bar
var idx = null
const docMap = new Map()

function prepareIdxAndDocMap() {
  const docs = [  
    {
      "title": "Creating Mails",
      "url": "/emil/doc/building",
      "content": "Creating Mails The Mail class in emil-common is used to represent an e-mail. Using the MailBuilder makes creating values more convenient. The MailBuilder works by collecting a list of transformations to an initial Mail object and applying them when invoking the build method. There exists a set of predefined transformations to cover the most common things. But you can create your own easily, too. Simple Mails Creating mails without attachments: import cats.effect._, emil._, emil.builder._ val mail: Mail[IO] = MailBuilder.build( From(\"me@test.com\"), To(\"test@test.com\"), Subject(\"Hello!\"), CustomHeader(Header(\"User-Agent\", \"my-email-client\")), TextBody(\"Hello!\\n\\nThis is a mail.\"), HtmlBody(\"&lt;h1&gt;Hello!&lt;/h1&gt;\\n&lt;p&gt;This &lt;b&gt;is&lt;/b&gt; a mail.&lt;/p&gt;\") ) // mail: Mail[IO] = Mail( // header = MailHeader( // id = \"\", // messageId = None, // folder = None, // recipients = Recipients( // to = List(MailAddress(name = None, address = \"test@test.com\")), // cc = List(), // bcc = List() // ), // sender = None, // from = Some(value = MailAddress(name = None, address = \"me@test.com\")), // replyTo = None, // originationDate = None, // subject = \"Hello!\", // received = List(), // flags = Set() // ), // additionalHeaders = Headers( // all = List( // Header( // name = \"User-Agent\", // value = NonEmptyList(head = \"my-email-client\", tail = List()) // ) // ) // ), // body = HtmlAndText( // text = Pure( // value = StringContent( // asString = \"\"\"Hello! // // This is a mail.\"\"\" // ) // ), // html = Pure( // value = StringContent( // asString = \"\"\"&lt;h1&gt;Hello!&lt;/h1&gt; // &lt;p&gt;This &lt;b&gt;is&lt;/b&gt; a mail.&lt;/p&gt;\"\"\" // ) // ) // ), // attachments = Attachments(all = Vector()) // ) The Mail defines an effect type, because the attachments and the mail body does. Using MailBuilder.build already applies all transformations and yields the final Mail instance. Using the MailBuilder.apply instead, would return the MailBuilder instance which can be further modified. It is also possible to go from an existing Mail to a MailBuilder to change certain parts: val builder = mail.asBuilder // builder: MailBuilder[IO] = emil.builder.MailBuilder@1791e231 val mail2 = builder. clearRecipients. add(To(\"me2@test.com\")). set(Subject(\"Hello 2\")). build // mail2: Mail[IO] = Mail( // header = MailHeader( // id = \"\", // messageId = None, // folder = None, // recipients = Recipients( // to = List(MailAddress(name = None, address = \"me2@test.com\")), // cc = List(), // bcc = List() // ), // sender = None, // from = Some(value = MailAddress(name = None, address = \"me@test.com\")), // replyTo = None, // originationDate = None, // subject = \"Hello 2\", // received = List(), // flags = Set() // ), // additionalHeaders = Headers( // all = List( // Header( // name = \"User-Agent\", // value = NonEmptyList(head = \"my-email-client\", tail = List()) // ) // ) // ), // body = HtmlAndText( // text = Pure( // value = StringContent( // asString = \"\"\"Hello! // // This is a mail.\"\"\" // ) // ), // html = Pure( // value = StringContent( // asString = \"\"\"&lt;h1&gt;Hello!&lt;/h1&gt; // &lt;p&gt;This &lt;b&gt;is&lt;/b&gt; a mail.&lt;/p&gt;\"\"\" // ) // ) // ), // attachments = Attachments(all = Vector()) // ) The add and set methods are both doing the same thing: appending transformations. Both names exists for better reading; i.e. a recipient is by default appended, but the subject is not. The methods accept a list of transformations, too. val mail3 = mail2.asBuilder. clearRecipients. add( To(\"me3@test.com\"), Cc(\"other@test.com\") ). build // mail3: Mail[IO] = Mail( // header = MailHeader( // id = \"\", // messageId = None, // folder = None, // recipients = Recipients( // to = List(MailAddress(name = None, address = \"me3@test.com\")), // cc = List(MailAddress(name = None, address = \"other@test.com\")), // bcc = List() // ), // sender = None, // from = Some(value = MailAddress(name = None, address = \"me@test.com\")), // replyTo = None, // originationDate = None, // subject = \"Hello 2\", // received = List(), // flags = Set() // ), // additionalHeaders = Headers( // all = List( // Header( // name = \"User-Agent\", // value = NonEmptyList(head = \"my-email-client\", tail = List()) // ) // ) // ), // body = HtmlAndText( // text = Pure( // value = StringContent( // asString = \"\"\"Hello! // // This is a mail.\"\"\" // ) // ), // html = Pure( // value = StringContent( // asString = \"\"\"&lt;h1&gt;Hello!&lt;/h1&gt; // &lt;p&gt;This &lt;b&gt;is&lt;/b&gt; a mail.&lt;/p&gt;\"\"\" // ) // ) // ), // attachments = Attachments(all = Vector()) // ) Mails with Attachments Adding attachments is the same as with other data. Creating attachments might be more involved, depending on where the data is coming from. Emil defines transformations to add attachments from files, urls and java’s InputStream easily. Otherwise you need to get a Stream[F, Byte] from somewhere else. import scala.concurrent.ExecutionContext val mail4 = mail3.asBuilder.add( AttachUrl[IO](getClass.getResource(\"/files/Test.pdf\")). withFilename(\"test.pdf\"). withMimeType(MimeType.pdf)). build // mail4: Mail[IO] = Mail( // header = MailHeader( // id = \"\", // messageId = None, // folder = None, // recipients = Recipients( // to = List(MailAddress(name = None, address = \"me3@test.com\")), // cc = List(MailAddress(name = None, address = \"other@test.com\")), // bcc = List() // ), // sender = None, // from = Some(value = MailAddress(name = None, address = \"me@test.com\")), // replyTo = None, // originationDate = None, // subject = \"Hello 2\", // received = List(), // flags = Set() // ), // additionalHeaders = Headers( // all = List( // Header( // name = \"User-Agent\", // value = NonEmptyList(head = \"my-email-client\", tail = List()) // ) // ) // ), // body = HtmlAndText( // text = Pure( // value = StringContent( // asString = \"\"\"Hello! // // This is a mail.\"\"\" // ) // ), // html = Pure( // value = StringContent( // asString = \"\"\"&lt;h1&gt;Hello!&lt;/h1&gt; // &lt;p&gt;This &lt;b&gt;is&lt;/b&gt; a mail.&lt;/p&gt;\"\"\" // ) // ) // ), // attachments = Attachments( // all = Vector( // Attachment( // filename = Some(value = \"test.pdf\"), // mimeType = MimeType( // primary = \"application\", // sub = \"pdf\", // params = Map() // ... Emil creates a Stream[F, Byte] from the java.net.URL using the fs2-io api. The same is available when attaching files and java.io.InputStreams. Any given Stream[F, Byte] can be attached as well: import fs2.Stream val mydata: Stream[IO, Byte] = Stream.empty.covary[IO] // mydata: Stream[IO, Byte] = Stream(..) val mail5 = mail4.asBuilder. clearAttachments. add( Attach(mydata). withFilename(\"empty.txt\") ). build // mail5: Mail[IO] = Mail( // header = MailHeader( // id = \"\", // messageId = None, // folder = None, // recipients = Recipients( // to = List(MailAddress(name = None, address = \"me3@test.com\")), // cc = List(MailAddress(name = None, address = \"other@test.com\")), // bcc = List() // ), // sender = None, // from = Some(value = MailAddress(name = None, address = \"me@test.com\")), // replyTo = None, // originationDate = None, // subject = \"Hello 2\", // received = List(), // flags = Set() // ), // additionalHeaders = Headers( // all = List( // Header( // name = \"User-Agent\", // value = NonEmptyList(head = \"my-email-client\", tail = List()) // ) // ) // ), // body = HtmlAndText( // text = Pure( // value = StringContent( // asString = \"\"\"Hello! // // This is a mail.\"\"\" // ) // ), // html = Pure( // value = StringContent( // asString = \"\"\"&lt;h1&gt;Hello!&lt;/h1&gt; // &lt;p&gt;This &lt;b&gt;is&lt;/b&gt; a mail.&lt;/p&gt;\"\"\" // ) // ) // ), // attachments = Attachments( // all = Vector( // Attachment( // filename = Some(value = \"empty.txt\"), // mimeType = MimeType( // primary = \"application\", // sub = \"octet-stream\", // params = Map() // ... Custom Transformations Customs transformations can be easily created. It’s only a function Mail[F] =&gt; Mail[F]. This can be handy, if there is a better way to retrieve attachments, or just to create common headers. object MyHeader { def apply[F[_]](value: String): Trans[F] = CustomHeader(\"X-My-Header\", value) } val mymail = MailBuilder.build[IO]( To(\"me@test.com\"), MyHeader(\"blablabla\"), TextBody(\"This is a text\") ) // mymail: Mail[IO] = Mail( // header = MailHeader( // id = \"\", // messageId = None, // folder = None, // recipients = Recipients( // to = List(MailAddress(name = None, address = \"me@test.com\")), // cc = List(), // bcc = List() // ), // sender = None, // from = None, // replyTo = None, // originationDate = None, // subject = \"\", // received = List(), // flags = Set() // ), // additionalHeaders = Headers( // all = List( // Header( // name = \"X-My-Header\", // value = NonEmptyList(head = \"blablabla\", tail = List()) // ) // ) // ), // body = Text(text = Pure(value = StringContent(asString = \"This is a text\"))), // attachments = Attachments(all = Vector()) // ) The above example simply delegates to an existing Trans constructor."
    } ,    
    {
      "title": "Concept",
      "url": "/emil/doc/concept",
      "content": "Concept Mail Model Emil uses a simplified model of an e-mail. With MIME, an e-mail can appear in various shapes. Emil uses a flat structure, being: Headers Body Attachments The Headers are the standard headers in an e-mail. The Body defines the content of the mail. It can be specified as plain text, HTML or both. In case both is specified it defines the same content, just a different format. It is translated into a multipart/alternative message. Then a list of attachments follows. The major difference to MIME is that it is not recursive. Emil can only create Mixed or Alternative MIME messages. MailOp The other concept is the MailOp, which is an alias for the cats Kleisli class, where the input type is a Connection. Every code that does something with an e-mail runs inside such a function. type MailOp[F[_], C, A] = Kleisli[F, C, A] The C is the type representing the connection to some mail server. It is not a concrete type, because this depends on the implementation and the operations should not depend on it. There are pre-defined primitive operations in the Access and Send trait, respectively. These are implemented by some “implementation module”. These primitive operations can be composed into custom ones. For example, this is an operation that moves the first mail in INBOX into the Trash folder: import cats.implicits._, cats.effect._, emil._ def moveToTrash[F[_]: Sync, C](a: Access[F, C]): MailOp[F, C, Unit] = { val trash = a.getOrCreateFolder(None, \"Trash\") val findFirstMail = a.getInbox. flatMap(in =&gt; a.search(in, 1)(SearchQuery.All)). map(_.mails.headOption.toRight(new Exception(\"No mail found.\"))). mapF(_.rethrow) for { target &lt;- trash mail &lt;- findFirstMail _ &lt;- a.moveMail(mail, target) } yield () } This uses only imports from emil-common. Implementation The module emil-common lets you define mails and operations among them. To actually execute these operations, an implementation module is necessary. Currently there is emil-javamail, that is based on the JavaMail library. This module provides a factory for creating concrete Connection instances. The emil-javamail module has a ConnectionResource that creates a Resource[F, JavaMailConnection] given some MailConfig. This can be used to run the MailOp operations. The Emil trait exists to make this more convenient. It simply combines the ConnectionResource and the implementations for the primitive MailOps (defined in Access and Send trait). The emil-javamail module provides this as JavaMailEmil. For example, to execute the “moveToTrash” operation from above, one needs a corresponding connection to some imap server and the JavaMailEmil. import emil.javamail._ import scala.concurrent.ExecutionContext val myemil = JavaMailEmil[IO]() // myemil: Emil[IO] = emil.javamail.JavaMailEmil@1621264e val imapConf = MailConfig(\"imap://devmail:143\", \"dev\", \"dev\", SSLType.NoEncryption) // imapConf: MailConfig = MailConfig( // url = \"imap://devmail:143\", // user = \"dev\", // password = \"dev\", // sslType = NoEncryption, // enableXOAuth2 = false, // disableCertificateCheck = false, // timeout = 10 seconds // ) val moveIO = myemil(imapConf).run(moveToTrash(myemil.access)) // moveIO: IO[Unit] = Uncancelable( // body = cats.effect.IO$$$Lambda$1486/0x00000008017023c0@5392c0a7, // event = cats.effect.tracing.TracingEvent$StackTrace // ) Note: The emil-javamail depends on the JavaMail library, which has a dual license: EPL and GPLv2 with Classpath exception."
    } ,    
    {
      "title": "Documentation",
      "url": "/emil/doc",
      "content": "Documentation Emil consists of multiple modules. The core is emil-common. It defines the data structures for representing an e-mail and the MailOp, a generic interface for “doing something with it”. There is one implementation module emil-javamail, which is currently based on the JavaMail library. There are several extension modules, that provide additional features based on third-party libraries."
    } ,    
    {
      "title": "Doobie Integration",
      "url": "/emil/ext/doobie",
      "content": "Doobie Integration The module emil-doobie provides Meta instances for Doobie. Usage With sbt: libraryDependencies += \"com.github.eikek\" %% \"emil-doobie\" % \"0.15.0\" Description You can use emil data types MailAddress, SSLType and Mail in your record classes. import emil._ case class Record(from: MailAddress, recipients: List[MailAddress], ssl: SSLType, mime: MimeType) In order to use these types in SQL, you need to import instances defined in emil.doobie.EmilDoobieMeta. import emil.doobie.EmilDoobieMeta._ import _root_.doobie.implicits._ val r = Record( MailAddress.unsafe(Some(\"Mr. Me\"), \"me@example.com\"), List( MailAddress.unsafe(Some(\"Mr. Mine\"), \"mine_2@example.com\"), MailAddress.unsafe(Some(\"Mr. Me\"), \"me@example.com\"), MailAddress.unsafe(Some(\"Mr. You\"), \"you@example.com\") ), SSLType.StartTLS, MimeType.textHtml ) // r: Record = Record( // from = MailAddress(name = Some(value = \"Mr. Me\"), address = \"me@example.com\"), // recipients = List( // MailAddress(name = Some(value = \"Mr. Mine\"), address = \"mine_2@example.com\"), // MailAddress(name = Some(value = \"Mr. Me\"), address = \"me@example.com\"), // MailAddress(name = Some(value = \"Mr. You\"), address = \"you@example.com\") // ), // ssl = StartTLS, // mime = MimeType( // primary = \"text\", // sub = \"html\", // params = Map(\"charset\" -&gt; \"UTF-8\") // ) // ) val insertRecord = sql\"\"\" insert into mailaddress(sender, recipients, ssl) values(${r.from}, ${r.recipients}, ${r.ssl}, ${r.mime}) \"\"\".update.run // insertRecord: &lt;none&gt;.&lt;root&gt;.doobie.package.ConnectionIO[Int] = Suspend( // a = Uncancelable( // body = cats.effect.kernel.MonadCancel$$Lambda$2255/0x0000000801886d60@677b5145 // ) // )"
    } ,    
    {
      "title": "Examples",
      "url": "/emil/examples",
      "content": "Examples First, some imports and setup: import cats.effect._ import emil._, emil.builder._ /* javamail backend */ import emil.javamail._ Creating an e-mail Create a simple mail: val mail: Mail[IO] = MailBuilder.build( From(\"me@test.com\"), To(\"test@test.com\"), Subject(\"Hello!\"), CustomHeader(Header(\"User-Agent\", \"my-email-client\")), TextBody(\"Hello!\\n\\nThis is a mail.\"), HtmlBody(\"&lt;h1&gt;Hello!&lt;/h1&gt;\\n&lt;p&gt;This &lt;b&gt;is&lt;/b&gt; a mail.&lt;/p&gt;\"), AttachUrl[IO](getClass.getResource(\"/files/Test.pdf\")). withFilename(\"test.pdf\"). withMimeType(MimeType.pdf) ) // mail: Mail[IO] = Mail( // header = MailHeader( // id = \"\", // messageId = None, // folder = None, // recipients = Recipients( // to = List(MailAddress(name = None, address = \"test@test.com\")), // cc = List(), // bcc = List() // ), // sender = None, // from = Some(value = MailAddress(name = None, address = \"me@test.com\")), // replyTo = None, // originationDate = None, // subject = \"Hello!\", // received = List(), // flags = Set() // ), // additionalHeaders = Headers( // all = List( // Header( // name = \"User-Agent\", // value = NonEmptyList(head = \"my-email-client\", tail = List()) // ) // ) // ), // body = HtmlAndText( // text = Pure( // value = StringContent( // asString = \"\"\"Hello! // // This is a mail.\"\"\" // ) // ), // html = Pure( // value = StringContent( // asString = \"\"\"&lt;h1&gt;Hello!&lt;/h1&gt; // &lt;p&gt;This &lt;b&gt;is&lt;/b&gt; a mail.&lt;/p&gt;\"\"\" // ) // ) // ), // attachments = Attachments( // all = Vector( // Attachment( // filename = Some(value = \"test.pdf\"), // mimeType = MimeType( // primary = \"application\", // sub = \"pdf\", // params = Map() // ... Sending Mails In order to do something with it, a connection to a server is necessary and a concrete emil: val myemil = JavaMailEmil[IO]() // myemil: Emil[IO] = emil.javamail.JavaMailEmil@11c80834 val smtpConf = MailConfig(\"smtp://devmail:25\", \"dev\", \"dev\", SSLType.NoEncryption) // smtpConf: MailConfig = MailConfig( // url = \"smtp://devmail:25\", // user = \"dev\", // password = \"dev\", // sslType = NoEncryption, // enableXOAuth2 = false, // disableCertificateCheck = false, // timeout = 10 seconds // ) Finally, create a program that sends the mail: val sendIO = myemil(smtpConf).send(mail) // sendIO: IO[cats.data.NonEmptyList[String]] = Uncancelable( // body = cats.effect.IO$$$Lambda$1486/0x00000008017023c0@22b771d1, // event = cats.effect.tracing.TracingEvent$StackTrace // ) Accessing Mails The JavaMail backend implements IMAP access to mailboxes. First, a connection to an imap server is necessary: val imapConf = MailConfig(\"imap://devmail:143\", \"dev\", \"dev\", SSLType.NoEncryption) // imapConf: MailConfig = MailConfig( // url = \"imap://devmail:143\", // user = \"dev\", // password = \"dev\", // sslType = NoEncryption, // enableXOAuth2 = false, // disableCertificateCheck = false, // timeout = 10 seconds // ) Then run an operation from the email.access interface: val readIO = myemil(imapConf).run(myemil.access.getInbox) // readIO: IO[MailFolder] = Uncancelable( // body = cats.effect.IO$$$Lambda$1486/0x00000008017023c0@3fd006e8, // event = cats.effect.tracing.TracingEvent$StackTrace // )"
    } ,    
    {
      "title": "Extension Modules",
      "url": "/emil/extensions",
      "content": "Extension Modules This is a list of extension modules to emil. They add more features or provide integration into on other libraries. To use them, you need to add them to your build. With sbt: libraryDependencies += \"com.github.eikek\" %% \"emil-{name}\" % \"0.15.0\" TNEF Extract TNEF attachments (a.k.a. winmail.dat). Doobie Provides meta instances for some emil types for Doobie. Markdown Use markdown for your mail bodies to create a text and html version. Jsoup Use Jsoup library to better deal with html e-mails."
    } ,    
    {
      "title": "Home",
      "url": "/emil/",
      "content": "E-Mail for Scala Emil is a library for dealing with E-Mail in Scala. The api builds on Cats and FS2. It comes with a backend implementation that is based on the well known Java Mail library. As such it is just another wrapper library, but also a bit different: Extensible DSL for creating mails in code. Conveniently send mails via SMTP. Search mail boxes via IMAP. The data structures model a simplified E-Mail structure. Instead of adhering to the recursive structure of a mime message, a mail here is flat, consisting of a header, a body (text, html or both) and a list of attachments. The data structures and api are in a separate module, that doesn’t depend on a concrete implementation library, like Java Mail. An implementation based on fs2-mail or even EWS can be created without affecting the user code of this library. Write your e-mail related code once and then decide how to execute. Usage With sbt, add the dependencies: \"com.github.eikek\" %% \"emil-common\" % \"0.15.0\" // the core library \"com.github.eikek\" %% \"emil-javamail\" % \"0.15.0\" // implementation module // … optionally, other modules analog Emil is provided for Scala 2.12, 2.13 and 3. Note: from 0.10.0 emil is builta against CE3. Also from this version onwards, support for scala3 has been added. There are extension modules that offer integration with other libraries or additional features based on third-party libraries. License This project is distributed under the MIT"
    } ,    
    {
      "title": "Jsoup",
      "url": "/emil/ext/jsoup",
      "content": "Jsoup The module emil-jsoup can be used for easier dealing with html mails. It is based on the famous jsoup library. Usage With sbt: libraryDependencies += \"com.github.eikek\" %% \"emil-jsoup\" % \"0.15.0\" Description This module provides the following: A custom transformation to the builder dsl to modify the html content in an e-mail. This allows to clean the html from unwanted content using a custom whitelist of allowed elements. For more information, please refer to the documentation of jsoup. Create a unified html view of a mail body. For the examples, consider the following mail: import cats.effect._ import emil._ import emil.builder._ val htmlMail = \"\"\"&lt;h1&gt;A header&lt;/h2&gt;&lt;p onclick=\"alert('hi!');\"&gt;Hello&lt;/p&gt;&lt;p&gt;World&lt;p&gt;\"\"\" // htmlMail: String = \"&lt;h1&gt;A header&lt;/h2&gt;&lt;p onclick=\\\"alert('hi!');\\\"&gt;Hello&lt;/p&gt;&lt;p&gt;World&lt;p&gt;\" val mail: Mail[IO] = MailBuilder.build( From(\"me@test.com\"), To(\"test@test.com\"), Subject(\"Hello!\"), HtmlBody(htmlMail) ) // mail: Mail[IO] = Mail( // header = MailHeader( // id = \"\", // messageId = None, // folder = None, // recipients = Recipients( // to = List(MailAddress(name = None, address = \"test@test.com\")), // cc = List(), // bcc = List() // ), // sender = None, // from = Some(value = MailAddress(name = None, address = \"me@test.com\")), // replyTo = None, // originationDate = None, // subject = \"Hello!\", // received = List(), // flags = Set() // ), // additionalHeaders = Headers(all = List()), // body = Html( // html = Pure( // value = StringContent( // asString = \"&lt;h1&gt;A header&lt;/h2&gt;&lt;p onclick=\\\"alert('hi!');\\\"&gt;Hello&lt;/p&gt;&lt;p&gt;World&lt;p&gt;\" // ) // ) // ), // attachments = Attachments(all = Vector()) // ) Cleaning HTML Note the evil onclick and the malformed html. A clean content can be created using the BodyClean transformation: import emil.jsoup._ val cleanMail = mail.asBuilder .add(BodyClean(EmailWhitelist.default)) .build // cleanMail: Mail[IO] = Mail( // header = MailHeader( // id = \"\", // messageId = None, // folder = None, // recipients = Recipients( // to = List(MailAddress(name = None, address = \"test@test.com\")), // cc = List(), // bcc = List() // ), // sender = None, // from = Some(value = MailAddress(name = None, address = \"me@test.com\")), // replyTo = None, // originationDate = None, // subject = \"Hello!\", // received = List(), // flags = Set() // ), // additionalHeaders = Headers(all = List()), // body = Html( // html = Map( // ioe = Pure( // value = StringContent( // asString = \"&lt;h1&gt;A header&lt;/h2&gt;&lt;p onclick=\\\"alert('hi!');\\\"&gt;Hello&lt;/p&gt;&lt;p&gt;World&lt;p&gt;\" // ) // ), // f = emil.jsoup.BodyClean$$$Lambda$2261/0x000000080188ec60@39626e48, // event = cats.effect.tracing.TracingEvent$StackTrace // ) // ), // attachments = Attachments(all = Vector()) // ) This creates a new mail where the body is annotated with a cleaning function. This only applies to html parts. When the body is now evaluated, the string looks now like this: import cats.effect.unsafe.implicits.global cleanMail.body.htmlPart.map(_.map(_.asString)).unsafeRunSync() // res0: Option[String] = Some( // value = \"&lt;html&gt;&lt;head&gt;&lt;meta charset=\\\"UTF-8\\\"&gt;&lt;/head&gt;&lt;body&gt;&lt;h1&gt;A header&lt;/h1&gt;&lt;p&gt;Hello&lt;/p&gt;&lt;p&gt;World&lt;/p&gt;&lt;p&gt;&lt;/p&gt;&lt;/body&gt;&lt;/html&gt;\" // ) Jsoup even fixes the invalid html tree. Html View The HtmlBodyView class can be used to create a unified view of an e-mail body. It produces HTML, converting a text-only body into html. For better results here, use the emil-markdown module. Example: val htmlView = HtmlBodyView( mail.body, Some(mail.header) ) // htmlView: IO[BodyContent] = Map( // ioe = Pure( // value = StringContent( // asString = \"&lt;h1&gt;A header&lt;/h2&gt;&lt;p onclick=\\\"alert('hi!');\\\"&gt;Hello&lt;/p&gt;&lt;p&gt;World&lt;p&gt;\" // ) // ), // f = emil.jsoup.HtmlBodyView$$$Lambda$2285/0x00000008018b2318@3db75056, // event = cats.effect.tracing.TracingEvent$StackTrace // ) If the mailHeader is given (second argument), a short header with the sender, receiver and subject is included into the result. The third argument is a config object HtmlBodyViewConfig that has a default value that contains: a function to convert a text-only body into html. This uses a very basic string replacement approach and also escapes html entities in the text. Use the emil-markdown module for more sophisticated text-to-html conversion. a datetime-formatter and a timezone to use when inserting the e-mail date into the document a function to modify the html document tree, which by defaults uses the cleaner from BodyClean to remove unwanted content The result of the example is: htmlView.map(_.asString).unsafeRunSync() // res1: String = \"\"\"&lt;html&gt;&lt;head&gt;&lt;meta charset=\"UTF-8\"&gt;&lt;/head&gt;&lt;body&gt;&lt;div style=\"padding-bottom: 0.8em;\"&gt; // &lt;strong&gt;From:&lt;/strong&gt; &lt;code&gt;me@test.com&lt;/code&gt;&lt;br&gt; // &lt;strong&gt;To:&lt;/strong&gt; &lt;code&gt;test@test.com&lt;/code&gt;&lt;br&gt; // &lt;strong&gt;Subject:&lt;/strong&gt; &lt;code&gt;Hello!&lt;/code&gt;&lt;br&gt; // &lt;strong&gt;Date:&lt;/strong&gt; &lt;code&gt;-&lt;/code&gt; // &lt;/div&gt; // &lt;h1&gt;A header&lt;/h1&gt;&lt;p&gt;Hello&lt;/p&gt;&lt;p&gt;World&lt;/p&gt;&lt;p&gt;&lt;/p&gt;&lt;/body&gt;&lt;/html&gt;\"\"\""
    } ,      
    {
      "title": "Markdown Bodies",
      "url": "/emil/ext/markdown",
      "content": "Markdown Bodies The module emil-markdown can be used to add markdown mail bodies. It provides an extension to the mail builder DSL and converts text via flexmark into html. The mail body then contains the text and html version. Usage With sbt: libraryDependencies += \"com.github.eikek\" %% \"emil-markdown\" % \"0.15.0\" Description This module provides a custom transformation to the builder dsl to create text and html mail-bodies by using markdown. import cats.effect._ import emil._ import emil.builder._ import emil.markdown._ val md = \"## Hello!\\n\\nThis is a *markdown* mail!\" val mail: Mail[IO] = MailBuilder.build( From(\"me@test.com\"), To(\"test@test.com\"), Subject(\"Hello!\"), // creates text and html body MarkdownBody(md) ) When this mail is send or serialized, the mail body is transformed into a multipart/alternative body with the markdown as a text/plain part and the generated html as a text/html part. Here is how it looks like in roundcube webmail:"
    } ,    
    {
      "title": "Obtaining Mails",
      "url": "/emil/doc/reading",
      "content": "Obtaining Mails The emil.Access trait defines primitive MailOps for retrieving mails and working with folders. Mails can retrieved by searching a folder. So at first a folder must be obtained. When searching mails, only MailHeaders are returned – the most common headers of an e-mail. More cane be loaded separately using the loadMail operation. A SearchQuery must be provided to the search operation. Using import emil.SearchQuery._ allows for conveniently defining these: import emil._, emil.SearchQuery._ import java.time._ val q = (ReceivedDate &gt;= Instant.now.minusSeconds(60)) &amp;&amp; (Subject =*= \"test\") &amp;&amp; !Flagged // q: And = And( // qs = List( // Not(c = Flagged), // ReceivedDate(date = 2023-11-18T11:55:18.043514104Z, rel = Ge), // Subject(text = \"test\") // ) // ) The subject test is a simple substring test. Look at SearchQuery to find out what is possible. Searching inbox may look like this: import cats.effect._ def searchInbox[C](a: Access[IO, C], q: SearchQuery) = for { inbox &lt;- a.getInbox mails &lt;- a.search(inbox, 20)(q) } yield mails The 20 defines a maximum count. Just use Int.MaxValue to return all. Returning mail headers only from a search saves bandwidth and is fast. If you really want to load all mails found, use the searchAndLoad operation. import cats.effect._ def searchLoadInbox[C](a: Access[IO, C], q: SearchQuery) = for { inbox &lt;- a.getInbox mails &lt;- a.searchAndLoad(inbox, 20)(q) } yield mails"
    } ,      
    {
      "title": "Syntax Utilities",
      "url": "/emil/doc/syntax",
      "content": "{{ page.title }} The emil-common module doesn’t provide utilities for parsing mails or mail addresses from text. This is provided by the emil-javamail module, because the implementation from JavaMail is used. There is a syntax object that has some common utilities when reading/writing e-mail. Parsing MimeType from String import emil.javamail.syntax._ import emil._ MimeType.parse(\"text/html; charset=utf-8\") // res0: Either[String, MimeType] = Right( // value = MimeType( // primary = \"text\", // sub = \"html\", // params = Map(\"charset\" -&gt; \"utf-8\") // ) // ) Reading/Writing E-Mail addresses Parse a string into an e-mail address: MailAddress.parse(\"John Doe &lt;jdoe@gmail.com&gt;\") // res1: Either[String, MailAddress] = Right( // value = MailAddress( // name = Some(value = \"John Doe\"), // address = \"jdoe@gmail.com\" // ) // ) MailAddress.parse(\"John Doe doe@com\") // res2: Either[String, MailAddress] = Left( // value = \"Invalid mail address 'John Doe doe@com' - Local address contains control or whitespace\" // ) Write the mail address as unicode or ascii-only string: val ma = MailAddress.parse(\"Örtlich &lt;über.uns@test.com&gt;\").toOption.get // ma: MailAddress = MailAddress( // name = Some(value = \"Örtlich\"), // address = \"über.uns@test.com\" // ) val ascii = ma.asAsciiString // ascii: String = \"\\\"Örtlich\\\" &lt;über.uns@test.com&gt;\" MailAddress.parse(ascii) // res3: Either[String, MailAddress] = Right( // value = MailAddress( // name = Some(value = \"Örtlich\"), // address = \"über.uns@test.com\" // ) // ) val unicode = ma.asUnicodeString // unicode: String = \"\\\"Örtlich\\\" &lt;über.uns@test.com&gt;\" E-Mail from/to String E-Mail as string: import cats.effect._ import emil._, emil.builder._ import cats.effect.unsafe.implicits.global val mail = MailBuilder.build[IO]( From(\"test@test.com\"), To(\"test@test.com\"), Subject(\"Hello\"), HtmlBody(\"&lt;p&gt;This is html&lt;/p&gt;\") ) // mail: Mail[IO] = Mail( // header = MailHeader( // id = \"\", // messageId = None, // folder = None, // recipients = Recipients( // to = List(MailAddress(name = None, address = \"test@test.com\")), // cc = List(), // bcc = List() // ), // sender = None, // from = Some(value = MailAddress(name = None, address = \"test@test.com\")), // replyTo = None, // originationDate = None, // subject = \"Hello\", // received = List(), // flags = Set() // ), // additionalHeaders = Headers(all = List()), // body = Html( // html = Pure(value = StringContent(asString = \"&lt;p&gt;This is html&lt;/p&gt;\")) // ), // attachments = Attachments(all = Vector()) // ) import emil.javamail.syntax._ val mailStr = mail.serialize.unsafeRunSync() // mailStr: String = \"\"\"Date: Sat, 18 Nov 2023 12:56:18 +0100 (CET) // From: test@test.com // To: test@test.com // Message-ID: &lt;1821892477.0.1700308578568@kalamos&gt; // Subject: Hello // MIME-Version: 1.0 // Content-Type: text/html; charset=utf-8 // Content-Transfer-Encoding: 7bit // // &lt;p&gt;This is html&lt;/p&gt;\"\"\" Deserialize: val mail2 = Mail.deserialize[IO](mailStr).unsafeRunSync() // mail2: Mail[IO] = Mail( // header = MailHeader( // id = \"messageId:&lt;1821892477.0.1700308578568@kalamos&gt;\", // messageId = Some(value = \"&lt;1821892477.0.1700308578568@kalamos&gt;\"), // folder = None, // recipients = Recipients( // to = List(MailAddress(name = None, address = \"test@test.com\")), // cc = List(), // bcc = List() // ), // sender = None, // from = Some(value = MailAddress(name = None, address = \"test@test.com\")), // replyTo = None, // originationDate = Some(value = 2023-11-18T11:56:18Z), // subject = \"Hello\", // received = List(), // flags = Set() // ), // additionalHeaders = Headers( // all = List( // Header( // name = \"MIME-Version\", // value = NonEmptyList(head = \"1.0\", tail = List()) // ), // Header( // name = \"Content-Type\", // value = NonEmptyList(head = \"text/html; charset=utf-8\", tail = List()) // ), // Header( // name = \"Content-Transfer-Encoding\", // value = NonEmptyList(head = \"7bit\", tail = List()) // ) // ) // ), // body = Html( // html = Pure( // value = ByteContent( // bytes = Chunk( // bytes = View( // at = scodec.bits.ByteVector$AtArray@b9286ad, // offset = 0L, // size = 19L // ) // ), // charset = Some(value = UTF-8) // ) // ) // ), // attachments = Attachments(all = Vector()) // )"
    } ,    
    {
      "title": "Extract TNEF files",
      "url": "/emil/ext/tnef",
      "content": "{{ page.title }} The module emil-tnef can be used to extract winmail.dat (tnef files) that may occur as attachments. Usage With sbt: libraryDependencies += \"com.github.eikek\" %% \"emil-tnef\" % \"0.15.0\" Description It happens that some clients add an attachment, often called winmail.dat, that contains the complete message, or some attachments. Fortunately, the poi library can read these files. The emil-tnef module provides a convenient way to extract these attachments. import cats.effect._ import fs2.Stream import emil._ import emil.builder._ import emil.tnef._ // creating a mail with a TNEF attachment val winmailData: Stream[IO, Byte] = Stream.empty val mail = MailBuilder.build[IO]( From(\"me@example.com\"), To(\"me@example.com\"), Subject(\"test\"), TextBody[IO](\"hello\"), AttachStream[IO](winmailData, Some(\"winmail.dat\"), TnefMimeType.applicationTnef) ) // replaces each TNEF attachment with its content val mail2: IO[Mail[IO]] = TnefExtract.replace[IO](mail) // mail2.unsafeRunSync() The extraction must read in the tnef byte stream, therefore the return value is inside a F."
    }    
  ];

  idx = lunr(function () {
    this.ref("title");
    this.field("content");

    docs.forEach(function (doc) {
      this.add(doc);
    }, this);
  });

  docs.forEach(function (doc) {
    docMap.set(doc.title, doc.url);
  });
}

// The onkeypress handler for search functionality
function searchOnKeyDown(e) {
  const keyCode = e.keyCode;
  const parent = e.target.parentElement;
  const isSearchBar = e.target.id === "search-bar";
  const isSearchResult = parent ? parent.id.startsWith("result-") : false;
  const isSearchBarOrResult = isSearchBar || isSearchResult;

  if (keyCode === 40 && isSearchBarOrResult) {
    // On 'down', try to navigate down the search results
    e.preventDefault();
    e.stopPropagation();
    selectDown(e);
  } else if (keyCode === 38 && isSearchBarOrResult) {
    // On 'up', try to navigate up the search results
    e.preventDefault();
    e.stopPropagation();
    selectUp(e);
  } else if (keyCode === 27 && isSearchBarOrResult) {
    // On 'ESC', close the search dropdown
    e.preventDefault();
    e.stopPropagation();
    closeDropdownSearch(e);
  }
}

// Search is only done on key-up so that the search terms are properly propagated
function searchOnKeyUp(e) {
  // Filter out up, down, esc keys
  const keyCode = e.keyCode;
  const cannotBe = [40, 38, 27];
  const isSearchBar = e.target.id === "search-bar";
  const keyIsNotWrong = !cannotBe.includes(keyCode);
  if (isSearchBar && keyIsNotWrong) {
    // Try to run a search
    runSearch(e);
  }
}

// Move the cursor up the search list
function selectUp(e) {
  if (e.target.parentElement.id.startsWith("result-")) {
    const index = parseInt(e.target.parentElement.id.substring(7));
    if (!isNaN(index) && (index > 0)) {
      const nextIndexStr = "result-" + (index - 1);
      const querySel = "li[id$='" + nextIndexStr + "'";
      const nextResult = document.querySelector(querySel);
      if (nextResult) {
        nextResult.firstChild.focus();
      }
    }
  }
}

// Move the cursor down the search list
function selectDown(e) {
  if (e.target.id === "search-bar") {
    const firstResult = document.querySelector("li[id$='result-0']");
    if (firstResult) {
      firstResult.firstChild.focus();
    }
  } else if (e.target.parentElement.id.startsWith("result-")) {
    const index = parseInt(e.target.parentElement.id.substring(7));
    if (!isNaN(index)) {
      const nextIndexStr = "result-" + (index + 1);
      const querySel = "li[id$='" + nextIndexStr + "'";
      const nextResult = document.querySelector(querySel);
      if (nextResult) {
        nextResult.firstChild.focus();
      }
    }
  }
}

// Search for whatever the user has typed so far
function runSearch(e) {
  if (e.target.value === "") {
    // On empty string, remove all search results
    // Otherwise this may show all results as everything is a "match"
    applySearchResults([]);
  } else {
    const tokens = e.target.value.split(" ");
    const moddedTokens = tokens.map(function (token) {
      // "*" + token + "*"
      return token;
    })
    const searchTerm = moddedTokens.join(" ");
    const searchResults = idx.search(searchTerm);
    const mapResults = searchResults.map(function (result) {
      const resultUrl = docMap.get(result.ref);
      return { name: result.ref, url: resultUrl };
    })

    applySearchResults(mapResults);
  }

}

// After a search, modify the search dropdown to contain the search results
function applySearchResults(results) {
  const dropdown = document.querySelector("div[id$='search-dropdown'] > .dropdown-content.show");
  if (dropdown) {
    //Remove each child
    while (dropdown.firstChild) {
      dropdown.removeChild(dropdown.firstChild);
    }

    //Add each result as an element in the list
    results.forEach(function (result, i) {
      const elem = document.createElement("li");
      elem.setAttribute("class", "dropdown-item");
      elem.setAttribute("id", "result-" + i);

      const elemLink = document.createElement("a");
      elemLink.setAttribute("title", result.name);
      elemLink.setAttribute("href", result.url);
      elemLink.setAttribute("class", "dropdown-item-link");

      const elemLinkText = document.createElement("span");
      elemLinkText.setAttribute("class", "dropdown-item-link-text");
      elemLinkText.innerHTML = result.name;

      elemLink.appendChild(elemLinkText);
      elem.appendChild(elemLink);
      dropdown.appendChild(elem);
    });
  }
}

// Close the dropdown if the user clicks (only) outside of it
function closeDropdownSearch(e) {
  // Check if where we're clicking is the search dropdown
  if (e.target.id !== "search-bar") {
    const dropdown = document.querySelector("div[id$='search-dropdown'] > .dropdown-content.show");
    if (dropdown) {
      dropdown.classList.remove("show");
      document.documentElement.removeEventListener("click", closeDropdownSearch);
    }
  }
}
