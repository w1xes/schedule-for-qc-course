import { isNotReadySchedule, filterClassesArray } from './sheduleUtils';

describe('sheduleUtils', () => {
    // =========================================================
    describe('isNotReadySchedule', () => {
        describe('empty schedule', () => {
            it('should return true when schedule is empty and loading is false', () => {
                // Arrange
                const schedule = {};
                const loading = false;
                // Act
                const result = isNotReadySchedule(schedule, loading);
                // Assert
                expect(result).toBe(true);
            });

            it('should return false when schedule is empty but loading is true', () => {
                // Arrange
                const schedule = {};
                const loading = true;
                // Act
                const result = isNotReadySchedule(schedule, loading);
                // Assert
                expect(result).toBe(false);
            });

            it('should return true when schedule is an empty array and loading is false', () => {
                const result = isNotReadySchedule([], false);
                expect(result).toBe(true);
            });

            it('should return false when schedule is an empty array and loading is true', () => {
                const result = isNotReadySchedule([], true);
                expect(result).toBe(false);
            });
        });

        describe('non-empty schedule', () => {
            it('should return false when schedule has data and loading is false', () => {
                // Arrange
                const schedule = { monday: ['lesson1'] };
                const loading = false;
                // Act
                const result = isNotReadySchedule(schedule, loading);
                // Assert
                expect(result).toBe(false);
            });

            it('should return false when schedule has data and loading is true', () => {
                const schedule = { monday: ['lesson1'] };
                const result = isNotReadySchedule(schedule, true);
                expect(result).toBe(false);
            });
        });

        describe('null / falsy schedule', () => {
            it('should return true when schedule is null and loading is false', () => {
                const result = isNotReadySchedule(null, false);
                expect(result).toBe(true);
            });

            it('should return false when schedule is null and loading is true', () => {
                const result = isNotReadySchedule(null, true);
                expect(result).toBe(false);
            });

            it('should return true when schedule is undefined and loading is false', () => {
                const result = isNotReadySchedule(undefined, false);
                expect(result).toBe(true);
            });
        });
    });

    // =========================================================
    describe('filterClassesArray', () => {
        describe('empty array', () => {
            it('should return empty array when input is empty', () => {
                // Arrange
                const input = [];
                // Act
                const result = filterClassesArray(input);
                // Assert
                expect(result).toEqual([]);
            });
        });

        describe('array with unique elements', () => {
            it('should return same array when all ids are unique', () => {
                // Arrange
                const input = [{ id: 1 }, { id: 2 }, { id: 3 }];
                // Act
                const result = filterClassesArray(input);
                // Assert
                expect(result).toEqual([{ id: 1 }, { id: 2 }, { id: 3 }]);
                expect(result).toHaveLength(3);
            });
        });

        describe('array with duplicates', () => {
            it('should remove duplicate items with same id', () => {
                // Arrange
                const input = [{ id: 1 }, { id: 2 }, { id: 1 }, { id: 3 }, { id: 2 }];
                // Act
                const result = filterClassesArray(input);
                // Assert
                expect(result).toHaveLength(3);
                expect(result.map(i => i.id)).toEqual([1, 2, 3]);
            });

            it('should keep the first occurrence when duplicates exist', () => {
                // Arrange
                const input = [
                    { id: 1, name: 'first' },
                    { id: 1, name: 'second' },
                ];
                // Act
                const result = filterClassesArray(input);
                // Assert
                expect(result).toHaveLength(1);
                expect(result[0].name).toBe('first');
            });

            it('should handle array where all elements are duplicates of one', () => {
                const input = [{ id: 5 }, { id: 5 }, { id: 5 }];
                const result = filterClassesArray(input);
                expect(result).toHaveLength(1);
                expect(result[0].id).toBe(5);
            });
        });
    });
});
