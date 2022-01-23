package emil.javamail

import com.sun.mail.imap.{IMAPFolder, IMAPStore}
import jakarta.mail.{Folder, Store, Transport}

package object internal {
  type JavaMailConnection =
    JavaMailConnectionGeneric[Store, Transport, Folder]

  type JavaMailImapConnection =
    JavaMailConnectionGeneric[IMAPStore, Transport, IMAPFolder]
}
