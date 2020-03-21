package life.calgo.model;

import static java.util.Objects.requireNonNull;
import static life.calgo.commons.util.CollectionUtil.requireAllNonNull;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.logging.Logger;

import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

import life.calgo.commons.core.GuiSettings;
import life.calgo.commons.core.LogsCenter;
import life.calgo.model.day.DailyFoodLog;
import life.calgo.model.day.DailyGoal;
import life.calgo.model.food.ConsumedFood;
import life.calgo.model.food.Food;
import life.calgo.model.food.Name;

/**
 * Represents the in-memory model of the food record data.
 */
public class ModelManager implements Model {
    private static final Logger logger = LogsCenter.getLogger(ModelManager.class);

    private DailyGoal targetDailyCalories;
    private final FoodRecord foodRecord;
    private final UserPrefs userPrefs;
    private final FilteredList<Food> filteredFoods;
    private final FilteredList<ConsumedFood> currentFilteredDailyList;

    /**
     * Initializes a ModelManager with the given foodRecord and userPrefs.
     */
    public ModelManager(ReadOnlyFoodRecord readOnlyFoodRecord, ReadOnlyUserPrefs userPrefs, ReadOnlyGoal readOnlyGoal) {
        super();
        requireAllNonNull(readOnlyFoodRecord, userPrefs, readOnlyGoal);

        logger.fine("Initializing with food record: " + readOnlyFoodRecord + " and user prefs " + userPrefs
        + " and goal " + readOnlyGoal);

        this.foodRecord = new FoodRecord(readOnlyFoodRecord);
        this.userPrefs = new UserPrefs(userPrefs);
        this.targetDailyCalories = new DailyGoal(readOnlyGoal);
        filteredFoods = new FilteredList<>(this.foodRecord.getFoodList());
        currentFilteredDailyList = new FilteredList<>(this.foodRecord.getDailyList());
    }

    public ModelManager() {
        this(new FoodRecord(), new UserPrefs(), new DailyGoal());
    }

    //=========== UserPrefs ==================================================================================

    @Override
    public ReadOnlyUserPrefs getUserPrefs() {
        return userPrefs;
    }

    @Override
    public void setUserPrefs(ReadOnlyUserPrefs userPrefs) {
        requireNonNull(userPrefs);
        this.userPrefs.resetData(userPrefs);
    }

    @Override
    public GuiSettings getGuiSettings() {
        return userPrefs.getGuiSettings();
    }

    @Override
    public void setGuiSettings(GuiSettings guiSettings) {
        requireNonNull(guiSettings);
        userPrefs.setGuiSettings(guiSettings);
    }

    @Override
    public Path getFoodRecordFilePath() {
        return userPrefs.getFoodRecordFilePath();
    }

    @Override
    public void setFoodRecordFilePath(Path foodRecordFilePath) {
        requireNonNull(foodRecordFilePath);
        userPrefs.setFoodRecordFilePath(foodRecordFilePath);
    }

    //=========== FoodRecord ================================================================================

    @Override
    public ReadOnlyFoodRecord getFoodRecord() {
        return foodRecord;
    }

    @Override
    public void setFoodRecord(ReadOnlyFoodRecord foodRecord) {
        this.foodRecord.resetData(foodRecord);
    }

    @Override
    public boolean hasFood(Food food) {
        requireNonNull(food);
        return foodRecord.hasFood(food);
    }

    public Food getExistingFood(Food toGet) {
        requireNonNull(toGet);
        return foodRecord.getExistingFood(toGet);
    }

    @Override
    public void deleteFood(Food target) {
        foodRecord.removeFood(target);
    }

    @Override
    public void addFood(Food food) {
        foodRecord.addFood(food);
        updateFilteredFoodRecord(PREDICATE_SHOW_ALL_FOODS);
    }

    @Override
    public void setFood(Food target, Food editedFood) {
        requireAllNonNull(target, editedFood);

        foodRecord.setFood(target, editedFood);
    }

    //=========== Day Model classes================================================================================
    @Override
    public Optional<Food> getFoodByName(Name name) {
        return foodRecord.getFoodByName(name);
    }

    @Override
    public boolean hasLogWithSameDate(DailyFoodLog foodLog) {
        return foodRecord.hasLogWithSameDate(foodLog);
    }

    @Override
    public boolean hasLogWithSameDate(LocalDate date) {
        return foodRecord.hasLogWithSameDate(new DailyFoodLog().setDate(date));
    }

    @Override
    public void addLog(DailyFoodLog foodLog) {
        foodRecord.addLog(foodLog);
    }

    @Override
    public void updateLog(DailyFoodLog logToUpdate) {
        foodRecord.updateLog(logToUpdate);
    }

    @Override
    public DailyFoodLog getLogByDate(LocalDate localDate) {
        return foodRecord.getLogByDate(localDate);
    }

    /**
     * Updates ModelManager's DailyGoal to the new targetDailyCalories
     * @param targetDailyCalories the new targetted number of calories to consume each day by user
     * @return the updated DailyGoal object
     */
    public DailyGoal updateDailyGoal(int targetDailyCalories) {
        if (isGoalMade()) {
            this.targetDailyCalories = this.targetDailyCalories.updateDailyGoal(targetDailyCalories);
        } else {
            this.targetDailyCalories = new DailyGoal(targetDailyCalories);
        }
        return this.targetDailyCalories;
    }

    /**
     * Checks if goal already exists
     * @return true if there is already some goal
     */
    public boolean isGoalMade() {
        return this.targetDailyCalories != null;
    }

    public DailyGoal getDailyGoal() {
        return this.targetDailyCalories;
    }

    //=========== Filtered Food Record Accessors =============================================================

    /**
     * Returns an unmodifiable view of the list of {@code Food} backed by the internal list of
     * {@code versionedFoodRecord}
     */
    @Override
    public ObservableList<Food> getFilteredFoodRecord() {
        return filteredFoods;
    }

    @Override
    public void updateFilteredFoodRecord(Predicate<Food> predicate) {
        requireNonNull(predicate);
        filteredFoods.setPredicate(predicate);
    }

    @Override
    public ObservableList<ConsumedFood> getCurrentFilteredDailyList() {
        return currentFilteredDailyList;
    }

    @Override
    public void updateCurrentFilteredDailyList(Predicate<ConsumedFood> predicate, LocalDate date) {
        requireNonNull(predicate);
        foodRecord.setDailyList(date);
        currentFilteredDailyList.setPredicate(predicate);
    }

    @Override
    public boolean equals(Object obj) {
        // short circuit if same object
        if (obj == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(obj instanceof ModelManager)) {
            return false;
        }

        // state check
        ModelManager other = (ModelManager) obj;
        return foodRecord.equals(other.foodRecord)
                && userPrefs.equals(other.userPrefs)
                && filteredFoods.equals(other.filteredFoods);
    }

}
