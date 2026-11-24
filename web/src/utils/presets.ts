import type { Scenario } from '../types';

export type ScenarioPreset = {
  id: string;
  name: string;
  description: string;
  scenario: Scenario;
};

const baseScenario = (overrides: Partial<Scenario>): Scenario => ({
  schemaVersion: 1,
  name: 'QueueFlux experiment',
  durationMicros: 60_000_000,
  seed: 12345,
  queues: [{ id: 'work-queue', maxDepth: 5000, discipline: 'FIFO' }],
  workerPools: [{
    id: 'workers',
    queueId: 'work-queue',
    workerCount: 5,
    concurrencyPerWorker: 1,
    serviceTime: {
      distribution: 'EXPONENTIAL',
      constantMicros: 250_000,
      meanMicros: 250_000
    }
  }],
  workloads: [{
    id: 'arrivals',
    queueId: 'work-queue',
    startMicros: 0,
    endMicros: 60_000_000,
    arrivals: { mode: 'POISSON', ratePerSecond: 20 }
  }],
  ...overrides
});

export const SCENARIO_PRESETS: ScenarioPreset[] = [
  {
    id: 'steady-state',
    name: 'Steady state',
    description: 'Balanced arrival rate and worker capacity for a baseline run.',
    scenario: baseScenario({ name: 'Steady-state baseline', seed: 1001 })
  },
  {
    id: 'saturation',
    name: 'Saturation',
    description: 'Push arrivals beyond worker capacity to expose backlog growth.',
    scenario: baseScenario({
      name: 'Worker saturation',
      seed: 2001,
      workerPools: [{
        id: 'workers',
        queueId: 'work-queue',
        workerCount: 3,
        concurrencyPerWorker: 1,
        serviceTime: {
          distribution: 'EXPONENTIAL',
          constantMicros: 250_000,
          meanMicros: 250_000
        }
      }]
    })
  },
  {
    id: 'burst',
    name: 'Burst traffic',
    description: 'Short, intense traffic with a larger worker pool to study recovery.',
    scenario: baseScenario({
      name: 'Burst traffic recovery',
      seed: 3001,
      durationMicros: 30_000_000,
      workerPools: [{
        id: 'workers',
        queueId: 'work-queue',
        workerCount: 8,
        concurrencyPerWorker: 1,
        serviceTime: {
          distribution: 'LOGNORMAL',
          constantMicros: 200_000,
          meanMicros: 200_000,
          stdDevMicros: 160_000
        }
      }],
      workloads: [{
        id: 'arrivals',
        queueId: 'work-queue',
        startMicros: 0,
        endMicros: 10_000_000,
        arrivals: { mode: 'CONSTANT', ratePerSecond: 80 }
      }]
    })
  },
  {
    id: 'slow-service',
    name: 'Slow service',
    description: 'Higher service-time variability to expose tail-latency behaviour.',
    scenario: baseScenario({
      name: 'Slow variable service',
      seed: 4001,
      workerPools: [{
        id: 'workers',
        queueId: 'work-queue',
        workerCount: 5,
        concurrencyPerWorker: 1,
        serviceTime: {
          distribution: 'LOGNORMAL',
          constantMicros: 400_000,
          meanMicros: 400_000,
          stdDevMicros: 300_000
        }
      }]
    })
  }
];

export function getScenarioPreset(id: string): Scenario {
  const preset = SCENARIO_PRESETS.find(candidate => candidate.id === id);
  if (!preset) throw new Error(`Unknown scenario preset: ${id}`);
  return structuredClone(preset.scenario);
}
