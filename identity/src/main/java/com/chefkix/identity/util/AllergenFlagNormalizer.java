package com.chefkix.identity.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Normalizes allergen identifiers at the identity boundary.
 *
 * <p>The UI uses stable identifiers, while older settings documents contain
 * labels such as {@code nuts} and {@code dairy}. Keeping this compatibility
 * mapping here gives the profile and settings APIs one canonical shape.</p>
 */
public final class AllergenFlagNormalizer {

  private static final int MAX_FLAGS = 100;
  private static final int MAX_FLAG_LENGTH = 100;

  private static final Map<String, String> LEGACY_ALIASES = Map.of(
      "nuts", "tree_nuts",
      "dairy", "milk",
      "shellfish", "crustaceans",
      "soy", "soybeans",
      "wheat", "cereals_gluten");

  private AllergenFlagNormalizer() {}

  public static List<String> normalize(Collection<String> flags) {
    if (flags == null || flags.isEmpty()) {
      return new ArrayList<>();
    }

    Map<String, String> unique = new LinkedHashMap<>();
    for (String rawFlag : flags) {
      if (rawFlag == null) {
        continue;
      }
      String flag = rawFlag.trim();
      if (flag.isEmpty()) {
        continue;
      }
      flag = LEGACY_ALIASES.getOrDefault(flag.toLowerCase(Locale.ROOT), flag);
      if (flag.length() > MAX_FLAG_LENGTH) {
        flag = flag.substring(0, MAX_FLAG_LENGTH);
      }
      unique.putIfAbsent(flag.toLowerCase(Locale.ROOT), flag);
      if (unique.size() == MAX_FLAGS) {
        break;
      }
    }
    return new ArrayList<>(unique.values());
  }
}
