import { messages } from './messages';
import type { FieldProblem, TaskResult } from './tasksApi';

export const TITLE_MAXIMUM = 150;
export const TASK_DESCRIPTION_MAXIMUM = 2000;

export interface TaskFieldMessages {
  title?: string;
  description?: string;
}

/**
 * Counts characters as the API does: Unicode characters of the trimmed text, not UTF-16 units.
 * `trim` also removes the no-break spaces that the API treats as blank (Assumption A-033).
 */
function length(text: string): number {
  return Array.from(text.trim()).length;
}

/** The rules of the API's limits (Assumption A-033), checked before asking, so the person sees them at once. */
export function validateTask(title: string, description: string): TaskFieldMessages {
  const found: TaskFieldMessages = {};
  if (length(title) === 0) {
    found.title = messages.titleRequired;
  } else if (length(title) > TITLE_MAXIMUM) {
    found.title = messages.titleTooLong;
  }
  if (length(description) > TASK_DESCRIPTION_MAXIMUM) {
    found.description = messages.taskDescriptionTooLong;
  }
  return found;
}

/** The messages for the members that the API refused, in the User interface's own words. */
export function taskFieldMessages(problems: FieldProblem[]): TaskFieldMessages {
  const found: TaskFieldMessages = {};
  for (const problem of problems) {
    if (problem.pointer === '/title') {
      found.title = problem.detail.includes('required') ? messages.titleRequired : messages.titleTooLong;
    } else if (problem.pointer === '/description') {
      found.description = messages.taskDescriptionTooLong;
    }
  }
  return found;
}

/** The text for a refusal that is not about a field, or `null` for a success. */
export function taskRefusal(result: TaskResult<unknown>): string | null {
  switch (result.outcome) {
    case 'ok':
      return null;
    case 'invalid':
      return messages.invalidTask;
    case 'notFound':
      return messages.taskNotFound;
    case 'forbidden':
      return messages.notProjectOwner;
    case 'completed':
      return messages.projectCompleted;
    default:
      return messages.somethingFailed;
  }
}
