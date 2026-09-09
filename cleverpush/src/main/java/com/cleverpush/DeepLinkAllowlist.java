package com.cleverpush;

import static com.cleverpush.Constants.LOG_TAG;

import android.content.Context;
import android.content.res.XmlResourceParser;

import com.cleverpush.util.Logger;

import org.xmlpull.v1.XmlPullParser;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Deep-link schemes/hosts declared by the host app in VIEW + BROWSABLE intent-filters.
 */
final class DeepLinkAllowlist {

  private static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";

  static final class Rule {
    final String scheme;
    final String host;

    Rule(String scheme, String host) {
      this.scheme = scheme;
      this.host = host;
    }
  }

  private static volatile List<Rule> cachedRules;

  private DeepLinkAllowlist() {
  }

  static boolean allows(Context context, String url) {
    if (context == null || url == null) {
      return false;
    }
    return matches(url, getRules(context));
  }

  static boolean matches(String url, List<Rule> rules) {
    if (url == null || rules == null || rules.isEmpty()) {
      return false;
    }
    try {
      URI uri = new URI(url.trim());
      String scheme = uri.getScheme();
      if (scheme == null || scheme.isEmpty()) {
        return false;
      }
      scheme = scheme.toLowerCase(Locale.US);
      String host = uri.getHost();
      if (host != null) {
        host = host.toLowerCase(Locale.US);
      }

      for (Rule rule : rules) {
        if (rule == null || rule.scheme == null || !rule.scheme.equals(scheme)) {
          continue;
        }
        if (hostMatches(rule.host, host)) {
          return true;
        }
      }
      return false;
    } catch (Exception e) {
      return false;
    }
  }

  static List<Rule> getRules(Context context) {
    if (cachedRules != null) {
      return cachedRules;
    }
    synchronized (DeepLinkAllowlist.class) {
      if (cachedRules == null) {
        cachedRules = Collections.unmodifiableList(loadRules(context));
        Logger.d(LOG_TAG, "DeepLinkAllowlist: loaded " + cachedRules.size() + " deep link rule(s)");
      }
    }
    return cachedRules;
  }

  private static List<Rule> loadRules(Context context) {
    XmlResourceParser parser = null;
    try {
      parser = context.getAssets().openXmlResourceParser("AndroidManifest.xml");
      return parseIntentFilters(parser);
    } catch (Exception e) {
      Logger.e(LOG_TAG, "DeepLinkAllowlist: failed to read host deep link intent-filters", e);
      return Collections.emptyList();
    } finally {
      if (parser != null) {
        parser.close();
      }
    }
  }

  static List<Rule> parseIntentFilters(XmlPullParser parser) throws Exception {
    List<Rule> rules = new ArrayList<>();
    boolean inIntentFilter = false;
    boolean isView = false;
    boolean isBrowsable = false;
    List<Rule> pending = new ArrayList<>();

    int eventType = parser.getEventType();
    while (eventType != XmlPullParser.END_DOCUMENT) {
      if (eventType == XmlPullParser.START_TAG) {
        String name = parser.getName();
        if ("intent-filter".equals(name)) {
          inIntentFilter = true;
          isView = false;
          isBrowsable = false;
          pending = new ArrayList<>();
        } else if (inIntentFilter && "action".equals(name)) {
          if ("android.intent.action.VIEW".equals(getAndroidAttr(parser, "name"))) {
            isView = true;
          }
        } else if (inIntentFilter && "category".equals(name)) {
          if ("android.intent.category.BROWSABLE".equals(getAndroidAttr(parser, "name"))) {
            isBrowsable = true;
          }
        } else if (inIntentFilter && "data".equals(name)) {
          String scheme = normalize(getAndroidAttr(parser, "scheme"));
          String host = normalize(getAndroidAttr(parser, "host"));
          if (scheme != null) {
            pending.add(new Rule(scheme, host));
          }
        }
      } else if (eventType == XmlPullParser.END_TAG && "intent-filter".equals(parser.getName())) {
        if (isView && isBrowsable) {
          for (Rule rule : pending) {
            if (isUsableRule(rule)) {
              rules.add(rule);
            }
          }
        }
        inIntentFilter = false;
      }
      eventType = parser.next();
    }
    return rules;
  }

  static boolean hostMatches(String ruleHost, String urlHost) {
    if (ruleHost == null || ruleHost.isEmpty() || "*".equals(ruleHost)) {
      return true;
    }
    return ruleHost.equals(urlHost)
        || (urlHost != null && ruleHost.startsWith("*.")
        && urlHost.length() > ruleHost.length() - 1
        && urlHost.endsWith(ruleHost.substring(1)));
  }

  static boolean isUsableRule(Rule rule) {
    if (rule == null || rule.scheme == null || rule.scheme.isEmpty()) {
      return false;
    }
    if ("http".equals(rule.scheme) || "https".equals(rule.scheme)) {
      return rule.host != null && !rule.host.isEmpty() && !"*".equals(rule.host);
    }
    return true;
  }

  private static String getAndroidAttr(XmlPullParser parser, String name) {
    String value = parser.getAttributeValue(ANDROID_NS, name);
    if (value != null && !value.isEmpty()) {
      return value;
    }
    for (int i = 0; i < parser.getAttributeCount(); i++) {
      if (name.equals(parser.getAttributeName(i))) {
        return parser.getAttributeValue(i);
      }
    }
    return null;
  }

  private static String normalize(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    if (trimmed.isEmpty()) {
      return null;
    }
    return trimmed.toLowerCase(Locale.US);
  }
}
