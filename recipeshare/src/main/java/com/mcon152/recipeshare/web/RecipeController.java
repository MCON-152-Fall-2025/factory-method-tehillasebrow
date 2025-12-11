package com.mcon152.recipeshare.web;

import com.mcon152.recipeshare.Recipe;
import com.mcon152.recipeshare.service.RecipeFactory;
import com.mcon152.recipeshare.service.RecipeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/recipes")
public class RecipeController {

    // 1. Static Logger Constant
    private static final Logger logger = LoggerFactory.getLogger(RecipeController.class);

    private final RecipeService recipeService;

    public RecipeController(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    @PostMapping
    public ResponseEntity<Recipe> addRecipe(@RequestBody RecipeRequest recipeRequest) {
        // 2. Trace Entry
        logger.info("POST /api/recipes - incoming create request");
        // Log Debug summary (Fix: use getTitle instead of getName)
        logger.debug("Incoming recipe body: title='{}', type='{}'",
                recipeRequest.getTitle(), recipeRequest.getType());

        try {
            Recipe toSave = RecipeFactory.createFromRequest(recipeRequest);

            // 3. MDC for tracing
            MDC.put("recipeName", toSave.getTitle());

            Recipe saved = recipeService.addRecipe(toSave);

            URI location = ServletUriComponentsBuilder
                    .fromCurrentRequest()
                    .path("/{id}")
                    .buildAndExpand(saved.getId())
                    .toUri();

            // 4. Trace Success
            logger.info("Created recipe with id={}", saved.getId());

            return ResponseEntity.created(location).body(saved);
        } catch (Exception e) {
            // 5. Trace Error with Exception
            logger.error("Error occurred while adding recipe: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        } finally {
            MDC.remove("recipeName");
        }
    }

    @GetMapping
    public ResponseEntity<List<Recipe>> getAllRecipes() {
        logger.info("GET /api/recipes - retrieving all recipes");
        return ResponseEntity.ok(recipeService.getAllRecipes());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Recipe> getRecipeById(@PathVariable long id) {
        logger.info("GET /api/recipes/{} - retrieving recipe", id);

        return recipeService.getRecipeById(id)
                .map(recipe -> {
                    logger.info("Returning recipe id={}", id);
                    return ResponseEntity.ok(recipe);
                })
                .orElseGet(() -> {
                    // 6. Trace 404 Warn
                    logger.warn("Recipe not found: id={}", id);
                    return ResponseEntity.notFound().build();
                });
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecipe(@PathVariable long id) {
        logger.info("DELETE /api/recipes/{} - delete requested", id);

        try {
            boolean deleted = recipeService.deleteRecipe(id);

            if (deleted) {
                logger.info("Deleted recipe id={}", id);
                return ResponseEntity.noContent().build();
            } else {
                logger.warn("Delete failed — recipe not found id={}", id);
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            logger.error("Unexpected error while deleting recipe id={}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Recipe> updateRecipe(@PathVariable long id, @RequestBody RecipeRequest updatedRequest) {
        logger.info("PUT /api/recipes/{} - full update requested", id);
        logger.debug("Update body: title='{}', type='{}'",
                updatedRequest.getTitle(), updatedRequest.getType());

        Recipe updatedRecipe = RecipeFactory.createFromRequest(updatedRequest);
        // Fix: getTitle instead of getName
        MDC.put("recipeName", updatedRecipe.getTitle());

        try {
            return recipeService.updateRecipe(id, updatedRecipe)
                    .map(r -> {
                        logger.info("Updated recipe id={}", id);
                        return ResponseEntity.ok(r);
                    })
                    .orElseGet(() -> {
                        logger.warn("Update failed — recipe not found id={}", id);
                        return ResponseEntity.notFound().build();
                    });
        } finally {
            MDC.remove("recipeName");
        }
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Recipe> patchRecipe(@PathVariable long id, @RequestBody RecipeRequest partialRequest) {
        logger.info("PATCH /api/recipes/{} - partial update requested", id);
        logger.debug("Patch body: title='{}', type='{}'",
                partialRequest.getTitle(), partialRequest.getType());

        Recipe partialRecipe = RecipeFactory.createFromRequest(partialRequest);
        // Fix: getTitle instead of getName
        MDC.put("recipeName", partialRecipe.getTitle());

        try {
            return recipeService.patchRecipe(id, partialRecipe)
                    .map(r -> {
                        logger.info("Patched recipe id={}", id);
                        return ResponseEntity.ok(r);
                    })
                    .orElseGet(() -> {
                        logger.warn("Patch failed — recipe not found id={}", id);
                        return ResponseEntity.notFound().build();
                    });
        } finally {
            MDC.remove("recipeName");
        }
    }
}