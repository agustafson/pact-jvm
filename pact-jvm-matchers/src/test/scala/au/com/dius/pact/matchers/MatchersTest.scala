package au.com.dius.pact.matchers

import au.com.dius.pact.model._
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

import scala.collection.JavaConversions

@RunWith(classOf[JUnitRunner])
class MatchersTest extends Specification {

  "matchers defined" should {

    "should be false when there are no matchers" in {
      Matchers.matcherDefined(Seq(""), None) must beFalse
    }

    "should be false when the path does not have a matcher entry" in {
      Matchers.matcherDefined(Seq("$", "body", "something"), Some(Map())) must beFalse
    }

    "should be true when the path does have a matcher entry" in {
      Matchers.matcherDefined(Seq("$", "body", "something"), Some(Map("$.body.something" -> Map[String, String]()))) must beTrue
    }

    "should be true when a parent of the path has a matcher entry" in {
      Matchers.matcherDefined(Seq("$", "body", "something"), Some(Map("$.body" -> Map[String, String]()))) must beTrue
    }

  }

  "should default to equality matching if the matcher is unknown" in {
    Matchers.matcher(Map("other" -> "something")) must be(EqualsMatcher)
    Matchers.matcher(Map()) must be(EqualsMatcher)
  }

  "should default to a matching defined at a parent level" in {
    Matchers.selectBestMatcher(Some(Map("$.body" -> Map("match" -> "type"))), Seq("$", "body", "value")) must beEqualTo(Map("match" -> "type"))
  }

  "equal matcher" should {

    "match using equals" in {
      EqualsMatcher.domatch[BodyMismatch](null, Seq("/"), "100", "100", BodyMismatchFactory).isEmpty must beTrue
      EqualsMatcher.domatch[BodyMismatch](null, Seq("/"), 100, "100", BodyMismatchFactory).isEmpty must beFalse
    }

  }

  "regex matcher" should {

    "match using the provided regex" in {
      val expected = new Request("get", "/", null, null, "{\"value\": \"Harry\"}", CollectionUtils.scalaMMapToJavaMMap(Map("$.body.value" -> Map("regex" -> "Ha[a-z]*"))))
      val actual = new Request("get", "/", null, null, "{\"value\": \"Harry\"}", null)
      new JsonBodyMatcher().matchBody(expected, actual, DiffConfig()) must beEmpty
    }

    "handle null values" in {
      val expected = new Request("get", "/", null, null, "{\"value\": \"Harry\"}", CollectionUtils.scalaMMapToJavaMMap(Map("$.body.value" -> Map("regex" -> "Ha[a-z]*"))))
      val actual = new Request("get", "/", null, null, "{\"value\": null}", null)
      new JsonBodyMatcher().matchBody(expected, actual, DiffConfig()) must not(beEmpty)
    }

  }

  "type matcher" should {

    "match on type" should {

      "accept strings" in {
        val expected = new Request("get", "/", null, null, "{\"value\": \"Harry\"}", CollectionUtils.scalaMMapToJavaMMap(Map("$.body.value" -> Map("match" -> "type"))))
        val actual = new Request("get", "/", null, null, "{\"value\": \"Some other string\"}", null)
        new JsonBodyMatcher().matchBody(expected, actual, DiffConfig()) must beEmpty
      }

      "accept numbers" in {
        val expected = new Request("get", "/", null, null, "{\"value\": 100}", CollectionUtils.scalaMMapToJavaMMap(Map("$.body.value" -> Map("match" -> "type"))))
        val actual = new Request("get", "/", null, null, "{\"value\": 200.3}", null)
        new JsonBodyMatcher().matchBody(expected, actual, DiffConfig()) must beEmpty
      }

      "accept booleans" in {
        val expected = new Request("get", "/", null, null, "{\"value\": true}", CollectionUtils.scalaMMapToJavaMMap(Map("$.body.value" -> Map("match" -> "type"))))
        val actual = new Request("get", "/", null, null, "{\"value\": false}", null)
        new JsonBodyMatcher().matchBody(expected, actual, DiffConfig()) must beEmpty
      }

      "accept null" in {
        val expected = new Request("get", "/", null, null, "{\"value\": null}", CollectionUtils.scalaMMapToJavaMMap(Map("$.body.value" -> Map("match" -> "type"))))
        val actual = new Request("get", "/", null, null, "{\"value\": null}", null)
        new JsonBodyMatcher().matchBody(expected, actual, DiffConfig()) must beEmpty
      }

      "not accept different types" in {
        val expected = new Request("get", "/", null, null, "{\"value\": \"200\"}", CollectionUtils.scalaMMapToJavaMMap(Map("$.body.value" -> Map("match" -> "type"))))
        val actual = new Request("get", "/", null, null, "{\"value\": 200}", null)
        new JsonBodyMatcher().matchBody(expected, actual, DiffConfig()) must not(beEmpty)
      }

      "not accept null/non-null" in {
        val expected = new Request("get", "/", null, null, "{\"value\": 200}", CollectionUtils.scalaMMapToJavaMMap(Map("$.body.value" -> Map("match" -> "type"))))
        val actual = new Request("get", "/", null, null, "{\"value\": null}", null)
        new JsonBodyMatcher().matchBody(expected, actual, DiffConfig()) must not(beEmpty)
      }

      "accept lists" in {
        val expected = new Request("get", "/", null, null, "{\"value\": [100, 200, 300]}", CollectionUtils.scalaMMapToJavaMMap(Map("$.body.value" -> Map("match" -> "type"))))
        val actual = new Request("get", "/", null, null, "{\"value\": [200.3]}", null)
        new JsonBodyMatcher().matchBody(expected, actual, DiffConfig()) must beEmpty
      }

      "accept maps" in {
        val expected = new Request("get", "/", null, null, "{\"value\": {\"a\": 100}}", CollectionUtils.scalaMMapToJavaMMap(Map("$.body.value" -> Map("match" -> "type"))))
        val actual = new Request("get", "/", null, null, "{\"value\": {\"a\": 200.3, \"b\": 200, \"c\": 300} }", null)
        new JsonBodyMatcher().matchBody(expected, actual, DiffConfig()) must beEmpty
      }

      "list elements should inherit the matcher from the parent" in {
        val expected = new Request("get", "/", null, null, "{\"value\": [100]}", CollectionUtils.scalaMMapToJavaMMap(Map("$.body.value" -> Map("match" -> "type"))))
        val actual = new Request("get", "/", null, null, "{\"value\": [\"200.3\"]}", null)
        new JsonBodyMatcher().matchBody(expected, actual, DiffConfig()) must not(beEmpty)
      }

      "map elements should inherit the matchers from the parent" in {
        val expected = new Request("get", "/", null, null, "{\"value\": {\"a\": 100}}", CollectionUtils.scalaMMapToJavaMMap(Map("$.body.value" -> Map("match" -> "type"))))
        val actual = new Request("get", "/", null, null, "{\"value\": {\"a\": \"200.3\", \"b\": 200, \"c\": 300} }", null)
        new JsonBodyMatcher().matchBody(expected, actual, DiffConfig()) must not(beEmpty)
      }

    }

    "match timestamps" should {

      "accept ISO formatted timestamps" in {
        val expected = new Request("get", "/", null, null, "{\"value\": \"2014-01-01 14:00:00+10:00\"}", CollectionUtils.scalaMMapToJavaMMap(Map("$.body.value" -> Map("match" -> "timestamp"))))
        val actual = new Request("get", "/", null, null, "{\"value\": \"2014-10-01 14:00:00+10:00\"}", null)
        new JsonBodyMatcher().matchBody(expected, actual, DiffConfig()) must beEmpty
      }

      "not accept incorrect formatted timestamps" in {
        val expected = new Request("get", "/", null, null, "{\"value\": \"2014-01-01 14:00:00\"}", CollectionUtils.scalaMMapToJavaMMap(Map("$.body.value" -> Map("match" -> "timestamp"))))
        val actual = new Request("get", "/", null, null, "{\"value\": \"I'm a timestamp!\"}", null)
        new JsonBodyMatcher().matchBody(expected, actual, DiffConfig()) must not(beEmpty)
      }

      "accept timestamps with custom patterns" in {
        val expected = new Request("get", "/", null, null, "{\"value\": \"2014-01-01-14:00:00+10:00\"}", CollectionUtils.scalaMMapToJavaMMap(Map("$.body.value" -> Map("timestamp" -> "yyyy-MM-dd-HH:mm:ssZZZ"))))
        val actual = new Request("get", "/", null, null, "{\"value\": \"2014-10-01-14:00:00+10:00\"}", null)
        new JsonBodyMatcher().matchBody(expected, actual, DiffConfig()) must beEmpty
      }

      "handle null values" in {
        val expected = new Request("get", "/", null, null, "{\"value\": \"2014-01-01-14:00:00+10:00\"}", CollectionUtils.scalaMMapToJavaMMap(Map("$.body.value" -> Map("timestamp" -> "yyyy-MM-dd-HH:mm:ssZZZ"))))
        val actual = new Request("get", "/", null, null, "{\"value\": null}", null)
        new JsonBodyMatcher().matchBody(expected, actual, DiffConfig()) must not(beEmpty)
      }

    }

    "match times" should {

      "not accept incorrect formatted times" in {
        val expected = new Request("get", "/", null, null, "{\"value\": \"00:00\"}", CollectionUtils.scalaMMapToJavaMMap(Map("$.body.value" -> Map("time" -> "mm:ss"))))
        val actual = new Request("get", "/", null, null, "{\"value\": \"14:01:02\"}", null)
        new JsonBodyMatcher().matchBody(expected, actual, DiffConfig()) must not(beEmpty)
      }

      "accept times with custom patterns" in {
        val expected = new Request("get", "/", null, null, "{\"value\": \"00:00:14\"}", CollectionUtils.scalaMMapToJavaMMap(Map("$.body.value" -> Map("time" -> "ss:mm:HH"))))
        val actual = new Request("get", "/", null, null, "{\"value\": \"05:10:14\"}", null)
        new JsonBodyMatcher().matchBody(expected, actual, DiffConfig()) must beEmpty
      }

      "handle null values" in {
        val expected = new Request("get", "/", null, null, "{\"value\": \"14:00:00\"}", CollectionUtils.scalaMMapToJavaMMap(Map("$.body.value" -> Map("time" -> "HH:mm:ss"))))
        val actual = new Request("get", "/", null, null, "{\"value\": null}", null)
        new JsonBodyMatcher().matchBody(expected, actual, DiffConfig()) must not(beEmpty)
      }

    }

    "match dates" should {

      "not accept incorrect formatted dates" in {
        val expected = new Request("get", "/", null, null, "{\"value\": \"01-01-1970\"}", CollectionUtils.scalaMMapToJavaMMap(Map("$.body.value" -> Map("date" -> "dd-MM-yyyy"))))
        val actual = new Request("get", "/", null, null, "{\"value\": \"01011970\"}", null)
        new JsonBodyMatcher().matchBody(expected, actual, DiffConfig()) must not(beEmpty)
      }

      "accept dates with custom patterns" in {
        val expected = new Request("get", "/", null, null, "{\"value\": \"12/30/1970\"}", CollectionUtils.scalaMMapToJavaMMap(Map("$.body.value" -> Map("date" -> "MM/dd/yyyy"))))
        val actual = new Request("get", "/", null, null, "{\"value\": \"12/30/1970\"}", null)
        new JsonBodyMatcher().matchBody(expected, actual, DiffConfig()) must beEmpty
      }

      "handle null values" in {
        val expected = new Request("get", "/", null, null, "{\"value\": \"2014-01-01\"}", CollectionUtils.scalaMMapToJavaMMap(Map("$.body.value" -> Map("date" -> "yyyy-MM-dd"))))
        val actual = new Request("get", "/", null, null, "{\"value\": null}", null)
        new JsonBodyMatcher().matchBody(expected, actual, DiffConfig()) must not(beEmpty)
      }

    }

  }

  "path matching" should {

    "match root node" in {
      Matchers.matchesPath("$", Seq("$")) must beTrue
      Matchers.matchesPath("$", Seq()) must beFalse
    }

    "match field name" in {
      Matchers.matchesPath("$.name", Seq("$", "name")) must beTrue
      Matchers.matchesPath("$.name.other", Seq("$", "name", "other")) must beTrue
      Matchers.matchesPath("$.name", Seq("$", "other")) must beFalse
      Matchers.matchesPath("$.name", Seq("$", "name", "other")) must beTrue
      Matchers.matchesPath("$.other", Seq("$", "name", "other")) must beFalse
      Matchers.matchesPath("$.name.other", Seq("$", "name")) must beFalse
    }

    "match array indices" in {
      Matchers.matchesPath("$[0]", Seq("$", "0")) must beTrue
      Matchers.matchesPath("$.name[1]", Seq("$", "name", "1")) must beTrue
      Matchers.matchesPath("$.name", Seq("$", "0")) must beFalse
      Matchers.matchesPath("$.name[1]", Seq("$", "name", "0")) must beFalse
      Matchers.matchesPath("$[1].name", Seq("$", "name", "1")) must beFalse
    }

    "match with wildcard" in {
      Matchers.matchesPath("$[*]", Seq("$", "0")) must beTrue
      Matchers.matchesPath("$.*", Seq("$", "name")) must beTrue
      Matchers.matchesPath("$.*.name", Seq("$", "some", "name")) must beTrue
      Matchers.matchesPath("$.name[*]", Seq("$", "name", "0")) must beTrue
      Matchers.matchesPath("$.name[*].name", Seq("$", "name", "1", "name")) must beTrue

      Matchers.matchesPath("$[*]", Seq("$", "str")) must beFalse
    }

  }

}
