package com.chefkix.culinary.features.knowledge.service;

import com.chefkix.culinary.features.knowledge.entity.KnowledgeIngredient;
import com.chefkix.culinary.features.knowledge.entity.KnowledgeTechnique;
import com.chefkix.culinary.features.knowledge.repository.KnowledgeIngredientRepository;
import com.chefkix.culinary.features.knowledge.repository.KnowledgeTechniqueRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Seeds kg_ingredients and kg_techniques on startup if empty.
 * Data migrated from AI service's INGREDIENT_SUBSTITUTIONS, TECHNIQUE_GUIDES, COMMON_MISTAKES.
 * Spec: CHEFKIX_MASTER_PLAN.md §Engine 2, Phase 1
 */
@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class KnowledgeGraphSeeder {

    KnowledgeIngredientRepository ingredientRepo;
    KnowledgeTechniqueRepository techniqueRepo;

    @Order(5)
    @EventListener(ApplicationReadyEvent.class)
    public void seed() {
        seedIngredients();
        seedTechniques();
    }

    private void seedIngredients() {
        if (ingredientRepo.count() > 0) {
            log.info("[KG] kg_ingredients already populated ({} docs), skipping seed", ingredientRepo.count());
            return;
        }
        log.info("[KG] Seeding kg_ingredients...");

        var ingredients = List.of(
                // ── From INGREDIENT_SUBSTITUTIONS (10 entries) + enriched ──
                KnowledgeIngredient.builder()
                        .canonicalName("butter")
                        .name("Butter")
                        .aliases(List.of("unsalted butter", "salted butter", "sweet cream butter"))
                        .category("dairy")
                        .commonUnits(List.of("tbsp", "cup", "stick", "g", "oz"))
                        .allergenFlags(List.of("dairy"))
                        .substitutions(List.of(
                                KnowledgeIngredient.Substitution.builder().alternative("olive oil").context("in saute and roasting").ratio(0.75).build(),
                                KnowledgeIngredient.Substitution.builder().alternative("coconut oil").context("in baking").ratio(1.0).build(),
                                KnowledgeIngredient.Substitution.builder().alternative("ghee").context("general, lactose-free alternative").ratio(1.0).build()
                        ))
                        .build(),

                KnowledgeIngredient.builder()
                        .canonicalName("eggs")
                        .name("Eggs")
                        .aliases(List.of("egg", "large egg", "chicken egg", "free-range egg"))
                        .category("protein")
                        .commonUnits(List.of("piece", "dozen"))
                        .allergenFlags(List.of("eggs"))
                        .substitutions(List.of(
                                KnowledgeIngredient.Substitution.builder().alternative("flax eggs").context("in baking (1 tbsp flax + 3 tbsp water per egg)").ratio(1.0).build(),
                                KnowledgeIngredient.Substitution.builder().alternative("chia eggs").context("in baking (1 tbsp chia + 3 tbsp water per egg)").ratio(1.0).build(),
                                KnowledgeIngredient.Substitution.builder().alternative("applesauce").context("in baking for moisture (1/4 cup per egg)").ratio(1.0).build()
                        ))
                        .build(),

                KnowledgeIngredient.builder()
                        .canonicalName("milk")
                        .name("Milk")
                        .aliases(List.of("whole milk", "2% milk", "skim milk", "cow's milk"))
                        .category("dairy")
                        .commonUnits(List.of("cup", "ml", "oz", "l"))
                        .allergenFlags(List.of("dairy"))
                        .substitutions(List.of(
                                KnowledgeIngredient.Substitution.builder().alternative("almond milk").context("general, lighter flavor").ratio(1.0).build(),
                                KnowledgeIngredient.Substitution.builder().alternative("oat milk").context("in baking and coffee, creamier").ratio(1.0).build(),
                                KnowledgeIngredient.Substitution.builder().alternative("coconut milk").context("in curries and rich dishes").ratio(1.0).build()
                        ))
                        .build(),

                KnowledgeIngredient.builder()
                        .canonicalName("flour")
                        .name("Flour")
                        .aliases(List.of("all-purpose flour", "AP flour", "plain flour", "white flour"))
                        .category("baking")
                        .commonUnits(List.of("cup", "g", "oz", "tbsp"))
                        .allergenFlags(List.of("gluten"))
                        .substitutions(List.of(
                                KnowledgeIngredient.Substitution.builder().alternative("almond flour").context("in low-carb baking, denser result").ratio(1.0).build(),
                                KnowledgeIngredient.Substitution.builder().alternative("coconut flour").context("in gluten-free baking (use 1/4 amount)").ratio(0.25).build(),
                                KnowledgeIngredient.Substitution.builder().alternative("gluten-free flour blend").context("general 1:1 replacement").ratio(1.0).build()
                        ))
                        .build(),

                KnowledgeIngredient.builder()
                        .canonicalName("sugar")
                        .name("Sugar")
                        .aliases(List.of("white sugar", "granulated sugar", "cane sugar", "table sugar"))
                        .category("baking")
                        .commonUnits(List.of("cup", "tbsp", "tsp", "g", "oz"))
                        .allergenFlags(List.of())
                        .substitutions(List.of(
                                KnowledgeIngredient.Substitution.builder().alternative("honey").context("in dressings and sauces (reduce liquid slightly)").ratio(0.75).build(),
                                KnowledgeIngredient.Substitution.builder().alternative("maple syrup").context("in baking and breakfast").ratio(0.75).build(),
                                KnowledgeIngredient.Substitution.builder().alternative("coconut sugar").context("general 1:1, lower glycemic").ratio(1.0).build(),
                                KnowledgeIngredient.Substitution.builder().alternative("stevia").context("for zero-calorie sweetening (much less needed)").ratio(0.01).build()
                        ))
                        .build(),

                KnowledgeIngredient.builder()
                        .canonicalName("soy sauce")
                        .name("Soy Sauce")
                        .aliases(List.of("light soy sauce", "dark soy sauce", "shoyu", "jiangyu"))
                        .category("condiment")
                        .commonUnits(List.of("tbsp", "tsp", "ml"))
                        .allergenFlags(List.of("soy", "gluten"))
                        .substitutions(List.of(
                                KnowledgeIngredient.Substitution.builder().alternative("tamari").context("gluten-free alternative, similar flavor").ratio(1.0).build(),
                                KnowledgeIngredient.Substitution.builder().alternative("coconut aminos").context("soy-free, sweeter and milder").ratio(1.0).build(),
                                KnowledgeIngredient.Substitution.builder().alternative("liquid aminos").context("general 1:1 replacement").ratio(1.0).build()
                        ))
                        .build(),

                KnowledgeIngredient.builder()
                        .canonicalName("heavy cream")
                        .name("Heavy Cream")
                        .aliases(List.of("whipping cream", "double cream", "heavy whipping cream", "thickened cream"))
                        .category("dairy")
                        .commonUnits(List.of("cup", "ml", "tbsp"))
                        .allergenFlags(List.of("dairy"))
                        .substitutions(List.of(
                                KnowledgeIngredient.Substitution.builder().alternative("coconut cream").context("in curries and soups, rich flavor").ratio(1.0).build(),
                                KnowledgeIngredient.Substitution.builder().alternative("cashew cream").context("in sauces, blend soaked cashews").ratio(1.0).build(),
                                KnowledgeIngredient.Substitution.builder().alternative("greek yogurt").context("in baking and dips, tangy").ratio(1.0).build()
                        ))
                        .build(),

                KnowledgeIngredient.builder()
                        .canonicalName("breadcrumbs")
                        .name("Breadcrumbs")
                        .aliases(List.of("bread crumbs", "dried breadcrumbs", "fresh breadcrumbs"))
                        .category("baking")
                        .commonUnits(List.of("cup", "tbsp", "g"))
                        .allergenFlags(List.of("gluten"))
                        .substitutions(List.of(
                                KnowledgeIngredient.Substitution.builder().alternative("panko").context("for crispier coating, Japanese-style").ratio(1.0).build(),
                                KnowledgeIngredient.Substitution.builder().alternative("crushed crackers").context("general coating alternative").ratio(1.0).build(),
                                KnowledgeIngredient.Substitution.builder().alternative("almond meal").context("gluten-free, nuttier flavor").ratio(1.0).build()
                        ))
                        .build(),

                KnowledgeIngredient.builder()
                        .canonicalName("buttermilk")
                        .name("Buttermilk")
                        .aliases(List.of("cultured buttermilk"))
                        .category("dairy")
                        .commonUnits(List.of("cup", "ml"))
                        .allergenFlags(List.of("dairy"))
                        .substitutions(List.of(
                                KnowledgeIngredient.Substitution.builder().alternative("milk + lemon juice").context("1 cup milk + 1 tbsp lemon, let sit 5 min").ratio(1.0).build(),
                                KnowledgeIngredient.Substitution.builder().alternative("milk + vinegar").context("1 cup milk + 1 tbsp white vinegar").ratio(1.0).build(),
                                KnowledgeIngredient.Substitution.builder().alternative("plain yogurt").context("thin with milk to buttermilk consistency").ratio(1.0).build()
                        ))
                        .build(),

                KnowledgeIngredient.builder()
                        .canonicalName("tomato paste")
                        .name("Tomato Paste")
                        .aliases(List.of("concentrated tomato", "tomato puree (double concentrate)"))
                        .category("condiment")
                        .commonUnits(List.of("tbsp", "can", "g"))
                        .allergenFlags(List.of())
                        .substitutions(List.of(
                                KnowledgeIngredient.Substitution.builder().alternative("tomato sauce (reduced)").context("simmer sauce until thick, use 3x amount").ratio(3.0).build(),
                                KnowledgeIngredient.Substitution.builder().alternative("ketchup").context("in a pinch, sweeter and less concentrated").ratio(1.5).build(),
                                KnowledgeIngredient.Substitution.builder().alternative("sun-dried tomatoes").context("blend into paste, intense flavor").ratio(1.0).build()
                        ))
                        .build(),

                // ── Additional common ingredients (enriched beyond Python dicts) ──

                KnowledgeIngredient.builder()
                        .canonicalName("garlic")
                        .name("Garlic")
                        .aliases(List.of("garlic clove", "fresh garlic", "minced garlic"))
                        .category("produce")
                        .commonUnits(List.of("clove", "tsp", "tbsp", "head"))
                        .allergenFlags(List.of())
                        .substitutions(List.of(
                                KnowledgeIngredient.Substitution.builder().alternative("garlic powder").context("1/8 tsp per clove, in dry rubs").ratio(0.125).build(),
                                KnowledgeIngredient.Substitution.builder().alternative("garlic paste").context("1:1 by volume").ratio(1.0).build()
                        ))
                        .build(),

                KnowledgeIngredient.builder()
                        .canonicalName("olive oil")
                        .name("Olive Oil")
                        .aliases(List.of("extra virgin olive oil", "EVOO", "virgin olive oil", "light olive oil"))
                        .category("oil")
                        .commonUnits(List.of("tbsp", "cup", "ml", "tsp"))
                        .allergenFlags(List.of())
                        .substitutions(List.of(
                                KnowledgeIngredient.Substitution.builder().alternative("avocado oil").context("high-heat cooking, neutral flavor").ratio(1.0).build(),
                                KnowledgeIngredient.Substitution.builder().alternative("vegetable oil").context("general cooking, neutral").ratio(1.0).build()
                        ))
                        .build(),

                KnowledgeIngredient.builder()
                        .canonicalName("onion")
                        .name("Onion")
                        .aliases(List.of("yellow onion", "white onion", "red onion", "sweet onion", "brown onion"))
                        .category("produce")
                        .commonUnits(List.of("piece", "cup", "g"))
                        .allergenFlags(List.of())
                        .substitutions(List.of(
                                KnowledgeIngredient.Substitution.builder().alternative("shallots").context("milder, more elegant flavor").ratio(1.5).build(),
                                KnowledgeIngredient.Substitution.builder().alternative("leeks").context("in soups, milder onion flavor").ratio(1.0).build()
                        ))
                        .build(),

                KnowledgeIngredient.builder()
                        .canonicalName("chicken")
                        .name("Chicken")
                        .aliases(List.of("chicken breast", "chicken thigh", "chicken drumstick", "whole chicken", "chicken leg"))
                        .category("protein")
                        .commonUnits(List.of("piece", "lb", "g", "kg", "oz"))
                        .allergenFlags(List.of())
                        .substitutions(List.of(
                                KnowledgeIngredient.Substitution.builder().alternative("tofu").context("vegetarian alternative, press well").ratio(1.0).build(),
                                KnowledgeIngredient.Substitution.builder().alternative("turkey").context("leaner, similar cooking times").ratio(1.0).build()
                        ))
                        .build(),

                KnowledgeIngredient.builder()
                        .canonicalName("rice")
                        .name("Rice")
                        .aliases(List.of("white rice", "brown rice", "jasmine rice", "basmati rice", "sushi rice", "long grain rice"))
                        .category("grain")
                        .commonUnits(List.of("cup", "g", "oz"))
                        .allergenFlags(List.of())
                        .substitutions(List.of(
                                KnowledgeIngredient.Substitution.builder().alternative("cauliflower rice").context("low-carb alternative").ratio(1.0).build(),
                                KnowledgeIngredient.Substitution.builder().alternative("quinoa").context("higher protein, similar cooking method").ratio(1.0).build()
                        ))
                        .build(),

                KnowledgeIngredient.builder()
                        .canonicalName("salt")
                        .name("Salt")
                        .aliases(List.of("table salt", "sea salt", "kosher salt", "flaky salt", "finishing salt"))
                        .category("spice")
                        .commonUnits(List.of("tsp", "tbsp", "pinch", "g"))
                        .allergenFlags(List.of())
                        .substitutions(List.of(
                                KnowledgeIngredient.Substitution.builder().alternative("soy sauce").context("adds salt + umami, reduce other liquids").ratio(1.0).build(),
                                KnowledgeIngredient.Substitution.builder().alternative("lemon juice").context("brightens food similarly to salt").ratio(1.0).build()
                        ))
                        .build(),

                KnowledgeIngredient.builder()
                        .canonicalName("black pepper")
                        .name("Black Pepper")
                        .aliases(List.of("pepper", "ground pepper", "cracked pepper", "peppercorn"))
                        .category("spice")
                        .commonUnits(List.of("tsp", "pinch", "g"))
                        .allergenFlags(List.of())
                        .substitutions(List.of(
                                KnowledgeIngredient.Substitution.builder().alternative("white pepper").context("milder, in light-colored sauces").ratio(1.0).build()
                        ))
                        .build(),

                KnowledgeIngredient.builder()
                        .canonicalName("fish sauce")
                        .name("Fish Sauce")
                        .aliases(List.of("nuoc mam", "nam pla", "patis"))
                        .category("condiment")
                        .commonUnits(List.of("tbsp", "tsp", "ml"))
                        .allergenFlags(List.of("fish"))
                        .substitutions(List.of(
                                KnowledgeIngredient.Substitution.builder().alternative("soy sauce + lime").context("vegetarian, less umami depth").ratio(1.0).build(),
                                KnowledgeIngredient.Substitution.builder().alternative("Worcestershire sauce").context("similar umami, different profile").ratio(1.0).build()
                        ))
                        .build(),

                KnowledgeIngredient.builder()
                        .canonicalName("coconut milk")
                        .name("Coconut Milk")
                        .aliases(List.of("full-fat coconut milk", "light coconut milk", "canned coconut milk"))
                        .category("dairy")
                        .commonUnits(List.of("cup", "ml", "can"))
                        .allergenFlags(List.of())
                        .substitutions(List.of(
                                KnowledgeIngredient.Substitution.builder().alternative("heavy cream").context("in non-dairy-free dishes, richer").ratio(1.0).build(),
                                KnowledgeIngredient.Substitution.builder().alternative("oat cream").context("vegan alternative, lighter").ratio(1.0).build()
                        ))
                        .build(),

                KnowledgeIngredient.builder()
                        .canonicalName("lemon")
                        .name("Lemon")
                        .aliases(List.of("lemon juice", "lemon zest", "fresh lemon"))
                        .category("produce")
                        .commonUnits(List.of("piece", "tbsp", "tsp", "ml"))
                        .allergenFlags(List.of())
                        .substitutions(List.of(
                                KnowledgeIngredient.Substitution.builder().alternative("lime").context("similar acidity, different flavor profile").ratio(1.0).build(),
                                KnowledgeIngredient.Substitution.builder().alternative("white vinegar").context("for acidity only, 1/2 the amount").ratio(0.5).build()
                        ))
                        .build(),

                KnowledgeIngredient.builder()
                        .canonicalName("ginger")
                        .name("Ginger")
                        .aliases(List.of("fresh ginger", "ginger root", "minced ginger"))
                        .category("produce")
                        .commonUnits(List.of("tsp", "tbsp", "inch", "g"))
                        .allergenFlags(List.of())
                        .substitutions(List.of(
                                KnowledgeIngredient.Substitution.builder().alternative("ground ginger").context("1/4 tsp per tbsp fresh").ratio(0.25).build()
                        ))
                        .build(),

                KnowledgeIngredient.builder()
                        .canonicalName("cilantro")
                        .name("Cilantro")
                        .aliases(List.of("coriander", "coriander leaves", "fresh coriander", "Chinese parsley"))
                        .category("produce")
                        .commonUnits(List.of("cup", "tbsp", "bunch"))
                        .allergenFlags(List.of())
                        .substitutions(List.of(
                                KnowledgeIngredient.Substitution.builder().alternative("parsley").context("for cilantro-averse, similar color").ratio(1.0).build(),
                                KnowledgeIngredient.Substitution.builder().alternative("Thai basil").context("in Asian dishes, different but aromatic").ratio(1.0).build()
                        ))
                        .build(),

                KnowledgeIngredient.builder()
                        .canonicalName("parmesan")
                        .name("Parmesan")
                        .aliases(List.of("parmigiano-reggiano", "parmesan cheese", "grated parmesan"))
                        .category("dairy")
                        .commonUnits(List.of("cup", "tbsp", "g", "oz"))
                        .allergenFlags(List.of("dairy"))
                        .substitutions(List.of(
                                KnowledgeIngredient.Substitution.builder().alternative("pecorino romano").context("sharper, saltier Italian hard cheese").ratio(1.0).build(),
                                KnowledgeIngredient.Substitution.builder().alternative("nutritional yeast").context("vegan, cheesy umami flavor").ratio(1.0).build()
                        ))
                        .build(),

                KnowledgeIngredient.builder()
                        .canonicalName("pasta")
                        .name("Pasta")
                        .aliases(List.of("spaghetti", "penne", "linguine", "fettuccine", "rigatoni", "fusilli", "macaroni"))
                        .category("grain")
                        .commonUnits(List.of("oz", "g", "lb", "cup"))
                        .allergenFlags(List.of("gluten"))
                        .substitutions(List.of(
                                KnowledgeIngredient.Substitution.builder().alternative("rice noodles").context("gluten-free, lighter texture").ratio(1.0).build(),
                                KnowledgeIngredient.Substitution.builder().alternative("zucchini noodles").context("low-carb, use spiralizer").ratio(1.0).build()
                        ))
                        .build(),

                KnowledgeIngredient.builder()
                        .canonicalName("tomato")
                        .name("Tomato")
                        .aliases(List.of("tomatoes", "cherry tomato", "roma tomato", "plum tomato", "canned tomato", "diced tomato"))
                        .category("produce")
                        .commonUnits(List.of("piece", "cup", "can", "g"))
                        .allergenFlags(List.of())
                        .substitutions(List.of(
                                KnowledgeIngredient.Substitution.builder().alternative("canned tomatoes").context("when fresh not available, drain for raw use").ratio(1.0).build(),
                                KnowledgeIngredient.Substitution.builder().alternative("roasted red peppers").context("in sauces, different but rich").ratio(1.0).build()
                        ))
                        .build()
        );

        ingredientRepo.saveAll(ingredients);
        log.info("[KG] Seeded {} ingredients into kg_ingredients", ingredients.size());
    }

    private void seedTechniques() {
        if (techniqueRepo.count() > 0) {
            log.info("[KG] kg_techniques already populated ({} docs), skipping seed", techniqueRepo.count());
            return;
        }
        log.info("[KG] Seeding kg_techniques...");

        var techniques = List.of(
                // ── From TECHNIQUE_GUIDES (16 entries) + COMMON_MISTAKES merged ──

                KnowledgeTechnique.builder()
                        .canonicalName("searing")
                        .name("Searing")
                        .description("Searing creates a flavorful crust by cooking food at high heat. Pat ingredients dry and ensure the pan is very hot before adding food for best results.")
                        .difficulty("intermediate")
                        .category("heat-based")
                        .relatedEquipment(List.of("Skillet", "Cast Iron Pan", "Tongs"))
                        .commonMistake("Don't move the meat too soon! Let it develop a crust before flipping.")
                        .visualCues(List.of("deep golden-brown crust", "sizzling sound when placed", "meat releases from pan when ready"))
                        .relatedCuisines(List.of("French", "American", "Italian"))
                        .build(),

                KnowledgeTechnique.builder()
                        .canonicalName("braising")
                        .name("Braising")
                        .description("Braising involves slow cooking in liquid at low temperatures. This technique breaks down tough fibers and creates tender, flavorful results.")
                        .difficulty("intermediate")
                        .category("heat-based")
                        .relatedEquipment(List.of("Dutch Oven", "Stock Pot", "Slow Cooker"))
                        .commonMistake("Don't rush it. Low and slow is the key - high heat will toughen the meat.")
                        .visualCues(List.of("meat falls apart with fork", "liquid reduced and glossy", "rich aroma filling kitchen"))
                        .relatedCuisines(List.of("French", "Italian", "Korean"))
                        .build(),

                KnowledgeTechnique.builder()
                        .canonicalName("emulsifying")
                        .name("Emulsifying")
                        .description("Emulsifying combines liquids that normally don't mix (like oil and vinegar). Add liquid slowly while whisking vigorously to create a stable mixture.")
                        .difficulty("advanced")
                        .category("preparation")
                        .relatedEquipment(List.of("Whisk", "Immersion Blender", "Mixing Bowl"))
                        .commonMistake("Adding oil too quickly breaks the emulsion. Drizzle slowly and whisk constantly.")
                        .visualCues(List.of("thick, creamy consistency", "no separation visible", "coats back of spoon"))
                        .relatedCuisines(List.of("French", "Mediterranean"))
                        .build(),

                KnowledgeTechnique.builder()
                        .canonicalName("deglazing")
                        .name("Deglazing")
                        .description("Deglazing uses liquid to lift browned bits from the pan bottom, creating a flavorful sauce. Add wine, stock, or water to a hot pan after searing.")
                        .difficulty("beginner")
                        .category("heat-based")
                        .relatedEquipment(List.of("Skillet", "Wooden Spoon"))
                        .commonMistake("Don't let the fond (browned bits) burn before deglazing. Work quickly after removing protein.")
                        .visualCues(List.of("steam rising when liquid hits pan", "browned bits dissolving", "liquid darkening"))
                        .relatedCuisines(List.of("French", "Italian"))
                        .build(),

                KnowledgeTechnique.builder()
                        .canonicalName("blanching")
                        .name("Blanching")
                        .description("Blanching briefly cooks vegetables in boiling water, then shocks them in ice water. This preserves color, texture, and nutrients.")
                        .difficulty("beginner")
                        .category("preparation")
                        .relatedEquipment(List.of("Stock Pot", "Colander", "Mixing Bowl"))
                        .commonMistake("Don't skip the ice bath! Without it, vegetables continue cooking and lose their snap.")
                        .visualCues(List.of("vibrant bright color", "crisp-tender texture", "immediate color change in ice bath"))
                        .relatedCuisines(List.of("French", "Japanese", "Italian"))
                        .build(),

                KnowledgeTechnique.builder()
                        .canonicalName("reducing")
                        .name("Reducing")
                        .description("Reducing concentrates flavors by evaporating liquid through simmering. The sauce will thicken and intensify as water content decreases.")
                        .difficulty("beginner")
                        .category("heat-based")
                        .relatedEquipment(List.of("Sauce Pan", "Wooden Spoon"))
                        .commonMistake("Watch carefully - a reduction can go from perfect to burnt in seconds. Stir occasionally.")
                        .visualCues(List.of("sauce coats spoon", "bubbles becoming smaller and thicker", "volume noticeably decreased"))
                        .relatedCuisines(List.of("French", "Italian", "Chinese"))
                        .build(),

                KnowledgeTechnique.builder()
                        .canonicalName("proofing")
                        .name("Proofing")
                        .description("Proofing allows yeast dough to rise before baking. Keep dough in a warm, draft-free environment for optimal fermentation.")
                        .difficulty("intermediate")
                        .category("baking")
                        .relatedEquipment(List.of("Mixing Bowl", "Kitchen Towel", "Oven (off, light on)"))
                        .commonMistake("Too hot kills yeast, too cold slows it. Aim for 75-80F (24-27C). Don't over-proof or dough collapses.")
                        .visualCues(List.of("dough doubled in size", "fingerprint slowly springs back", "airy and pillowy texture"))
                        .relatedCuisines(List.of("French", "Italian", "American"))
                        .build(),

                KnowledgeTechnique.builder()
                        .canonicalName("tempering")
                        .name("Tempering")
                        .description("Tempering gradually adjusts ingredient temperatures to prevent separation or curdling. Slowly add hot liquid to eggs while whisking constantly.")
                        .difficulty("advanced")
                        .category("preparation")
                        .relatedEquipment(List.of("Whisk", "Sauce Pan", "Ladle"))
                        .commonMistake("Dumping hot liquid directly into eggs will scramble them. Add a small amount first, whisk, then combine.")
                        .visualCues(List.of("smooth, lump-free mixture", "gradual temperature equalization", "no visible curdling"))
                        .relatedCuisines(List.of("French", "Indian", "Japanese"))
                        .build(),

                KnowledgeTechnique.builder()
                        .canonicalName("caramelizing")
                        .name("Caramelizing")
                        .description("Caramelizing browns natural sugars through low, slow cooking. Patience is key - rush the process and you'll get bitterness instead of sweetness.")
                        .difficulty("intermediate")
                        .category("heat-based")
                        .relatedEquipment(List.of("Skillet", "Sauce Pan", "Spatula"))
                        .commonMistake("Stirring too often prevents browning. Let onions sit undisturbed between stirs. True caramelization takes 30-45 min.")
                        .visualCues(List.of("deep amber color", "sweet aroma", "ingredients significantly reduced in volume"))
                        .relatedCuisines(List.of("French", "American", "Spanish"))
                        .build(),

                KnowledgeTechnique.builder()
                        .canonicalName("kneading")
                        .name("Kneading")
                        .description("Kneading develops gluten in dough through folding and stretching. This creates structure and elasticity for better texture.")
                        .difficulty("beginner")
                        .category("baking")
                        .relatedEquipment(List.of("Stand Mixer", "Cutting Board"))
                        .commonMistake("Over-kneading makes bread tough. Stop when dough passes the windowpane test - stretch thin enough to see light through.")
                        .visualCues(List.of("smooth, elastic dough", "springs back when poked", "no longer sticking to hands"))
                        .relatedCuisines(List.of("Italian", "French", "Indian"))
                        .build(),

                // ── Asian techniques ──

                KnowledgeTechnique.builder()
                        .canonicalName("stir-frying")
                        .name("Stir-Frying")
                        .description("High-heat, quick cooking that preserves texture and nutrients. Keep ingredients moving constantly in the wok - this is where wok hei (breath of wok) comes alive.")
                        .difficulty("intermediate")
                        .category("asian")
                        .relatedEquipment(List.of("Wok", "Spider Strainer", "Chopsticks"))
                        .commonMistake("Wok must be smoking hot before anything goes in. No heat, no wok hei. This is the way.")
                        .visualCues(List.of("visible wok hei smoke", "ingredients slightly charred at edges", "still crisp and vibrant"))
                        .relatedCuisines(List.of("Chinese", "Thai", "Vietnamese"))
                        .build(),

                KnowledgeTechnique.builder()
                        .canonicalName("pho making")
                        .name("Pho Making")
                        .description("The art of pho is in the broth - charred ginger and onion, star anise, cinnamon, and patience. Simmer bones for hours to coax out every layer of flavor. Skim often, the broth should be clear as intention.")
                        .difficulty("advanced")
                        .category("asian")
                        .relatedEquipment(List.of("Stock Pot", "Spider Strainer", "Ladle", "Mortar and Pestle"))
                        .commonMistake("Skim, skim, skim. Clear broth is honest broth. Cloudy means you rushed. Patience is the ingredient here.")
                        .visualCues(List.of("crystal clear broth", "rich golden color", "aromatic steam rising"))
                        .relatedCuisines(List.of("Vietnamese"))
                        .build(),

                KnowledgeTechnique.builder()
                        .canonicalName("nuoc cham balancing")
                        .name("Nuoc Cham Balancing")
                        .description("The soul of Vietnamese cuisine - balancing sweet, sour, salty, umami. Start with fish sauce, lime, sugar, and adjust until each taste meets without dominating. Like life, it's about harmony.")
                        .difficulty("intermediate")
                        .category("asian")
                        .relatedEquipment(List.of("Mixing Bowl", "Whisk"))
                        .commonMistake("Less is more at first - you can always add more nuoc mam, but you can't take it back. Taste as you go.")
                        .visualCues(List.of("slightly amber liquid", "balanced aroma of lime and fish sauce", "sweet-sour-salty in one taste"))
                        .relatedCuisines(List.of("Vietnamese"))
                        .build(),

                KnowledgeTechnique.builder()
                        .canonicalName("banh rolling")
                        .name("Banh Rolling")
                        .description("Wet rice paper just enough - too much and it tears, too little and it cracks. Roll tight but gentle, like wrapping a gift. Fresh herbs go in first, they're the star.")
                        .difficulty("beginner")
                        .category("asian")
                        .relatedEquipment(List.of("Cutting Board", "Shallow Dish"))
                        .commonMistake("Rice paper - quick dip, no soaking. Wet it like you mean it but don't drown it. It softens on the plate.")
                        .visualCues(List.of("translucent wrapper", "tight but not bursting", "colorful filling visible through paper"))
                        .relatedCuisines(List.of("Vietnamese"))
                        .build(),

                KnowledgeTechnique.builder()
                        .canonicalName("kimchi making")
                        .name("Kimchi Making")
                        .description("Fermentation is patience and respect for time. Salt the cabbage, let it weep. The paste is art - gochugaru, garlic, ginger, fish sauce. Each family has their secret ratio.")
                        .difficulty("intermediate")
                        .category("preservation")
                        .relatedEquipment(List.of("Mixing Bowl", "Rubber Gloves", "Mason Jar"))
                        .commonMistake("Trust the process. Kimchi, miso, nuoc mam - time is the chef. You just guide it.")
                        .visualCues(List.of("cabbage wilted from salting", "vibrant red from gochugaru", "bubbling during fermentation"))
                        .relatedCuisines(List.of("Korean"))
                        .build(),

                KnowledgeTechnique.builder()
                        .canonicalName("dashi making")
                        .name("Dashi Making")
                        .description("The foundation of Japanese flavor - kombu and katsuobushi. Never boil the kombu, coax the umami gently. Remove bonito flakes the moment they sink. This is restraint's reward.")
                        .difficulty("beginner")
                        .category("asian")
                        .relatedEquipment(List.of("Stock Pot", "Strainer"))
                        .commonMistake("Boiling kombu releases slippery compounds that cloud the dashi. Gentle, gentle heat. Respect the ingredient.")
                        .visualCues(List.of("pale golden liquid", "subtle sea aroma", "clear, not cloudy"))
                        .relatedCuisines(List.of("Japanese"))
                        .build()
        );

        techniqueRepo.saveAll(techniques);
        log.info("[KG] Seeded {} techniques into kg_techniques", techniques.size());
    }
}
