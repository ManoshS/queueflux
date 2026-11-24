import { describe, expect, it } from 'vitest';
import { getScenarioPreset, SCENARIO_PRESETS } from './presets';

describe('scenario presets', () => {
  it('provides unique preset ids', () => {
    const ids = SCENARIO_PRESETS.map(preset => preset.id);
    expect(new Set(ids).size).toBe(ids.length);
    expect(ids.length).toBeGreaterThanOrEqual(3);
  });

  it('returns valid schema-version-one scenarios', () => {
    for (const preset of SCENARIO_PRESETS) {
      const scenario = getScenarioPreset(preset.id);
      expect(scenario.schemaVersion).toBe(1);
      expect(scenario.queues.length).toBeGreaterThan(0);
      expect(scenario.workerPools.length).toBeGreaterThan(0);
      expect(scenario.workloads.length).toBeGreaterThan(0);
    }
  });

  it('returns an independent scenario copy', () => {
    const first = getScenarioPreset(SCENARIO_PRESETS[0].id);
    first.name = 'changed';
    first.queues[0].id = 'changed-queue';

    const second = getScenarioPreset(SCENARIO_PRESETS[0].id);
    expect(second.name).not.toBe('changed');
    expect(second.queues[0].id).not.toBe('changed-queue');
  });

  it('rejects unknown preset ids', () => {
    expect(() => getScenarioPreset('does-not-exist')).toThrow('Unknown scenario preset');
  });
});
