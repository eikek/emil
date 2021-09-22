package emil

import java.time.Instant

import munit._

class ReceivedTest extends FunSuite {

  test("parse1") {
    val rc1 = """from localhost.localdomain (unknown [IPv6:2000:333:151:2::333:ffff:fff])
  by mx-out-01a.sjc2.discourse.cloud (Postfix) with ESMTP id aaaaaaaaaaE
  for <x.yz@posteo.de>; Fri, 13 Mar 2020 22:31:50 +0000 (UTC)"""
    val Right(rh) = Received.parse(rc1)
    assertEquals(rh.findFirst("id"), Some("aaaaaaaaaaE"))
    assertEquals(rh.date, Instant.parse("2020-03-13T22:31:50Z"))
  }

  test("parse2") {
    val rc = """from proxy02.posteo.name ([127.0.0.1])
  by dovecot07.posteo.name (Dovecot) with LMTP id KdksBP5+bF7MgAIACjXI6Q
  for <eike.kettner@posteo.de>; Sat, 14 Mar 2020 08:00:14 +0100"""
    val Right(rh) = Received.parse(rc)
    assertEquals(rh.findFirst("id"), Some("KdksBP5+bF7MgAIACjXI6Q"))
    assertEquals(rh.date, Instant.parse("2020-03-14T07:00:14Z"))
  }

  test("parse3") {
    val rc = """from proxy02.maite.de ([127.0.0.1])
  by proxy02.maite.name (Dovecot) with LMTP id 83QSOxCBbF5WswMAGFAyLg
  ; Sat, 14 Mar 2020 08:55:58 +0100"""
    val Right(rh) = Received.parse(rc)
    assertEquals(rh.findFirst("id"), Some("83QSOxCBbF5WswMAGFAyLg"))
    assertEquals(rh.date, Instant.parse("2020-03-14T07:55:58Z"))
  }

  test("parse4") {
    val rc = List(
      """from exim by atxc.org with sa-checked (Exim 4.89)
  (envelope-from <bounces+2693180-247c-yeye.anteelg+lists.clojure=atxc.org@m.dripemail2.com>)
  id 1d03ZV-0000Q6-Ja
  for yeye+lists.clojure@atxc.org; Mon, 17 Apr 2017 10:07:50 +0000""",
      """from o2.m.dripemail2.com ([167.89.40.107])
  by atxc.org with esmtps (TLS1.2:ECDHE_RSA_AES_128_GCM_SHA256:128)
  (Exim 4.89)
  (envelope-from <bounces+2693180-247c-yeye.anteelg+lists.clojure=atxc.org@m.dripemail2.com>)
  id 1d03ZT-0000Q1-Da
  for yeye.anteelg+lists.clojure@atxc.org; Mon, 17 Apr 2017 10:07:49 +0000""",
      """from MjY5MzE4MA (ec2-54-174-109-188.compute-1.amazonaws.com [54.174.109.188])
  by ismtpd0004p1iad1.sendgrid.net (SG) with HTTP id UWOuw45HReqNnw4G3tI8cA
  for <yeye.anteelg+lists.clojure@atxc.org>; Mon, 17 Apr 2017 10:07:30.967 +0000 (UTC)"""
    )

    for (rv <- rc) {
      val Right(rh) = Received.parse(rv)
      assert(rh.findFirst("id").isDefined)
    }

  }

}
