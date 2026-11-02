import { messages } from './messages';
import type { FieldProblem, ProjectResult } from './projectsApi';

export const NAME_MAXIMUM = 100;
export const DESCRIPTION_MAXIMUM = 1000;

export interface FieldMessages {
  name?: string;
  description?: string;
}

/** Counts characters as the API does: Unicode characters of the trimmed text, not UTF-16 units. */
function length(text: string): number {
  return Array.from(text.trim()).length;
}

/** The rules of the API's limits (Assumption A-031), checked before asking, so the person sees them at once. */
export function validateProject(name: string, description: string): FieldMessages {
  const found: FieldMessages = {};
  if (length(name) === 0) {
    found.name = messages.nameRequired;
  } else if (length(name) > NAME_MAXIMUM) {
    found.name = messages.nameTooLong;
  }
  if (length(description) > DESCRIPTION_MAXIMUM) {
    found.description = messages.descriptionTooLong;
  }
  return found;
}

/** The messages for the members that the API refused, in the User interface's own words. */
export function fieldMessages(problems: FieldProblem[]): FieldMessages {
  const found: FieldMessages = {};
  for (const problem of problems) {
    if (problem.pointer === '/name') {
      found.name = problem.detail.includes('required') ? messages.nameRequired : messages.nameTooLong;
    } else if (problem.pointer === '/description') {
      found.description = messages.descriptionTooLong;
    }
  }
  return found;
}

/** The text for a refusal that is not about a field, or `null` for a success. */
export function refusal(result: ProjectResult<unknown>): string | null {
  switch (result.outcome) {
    case 'ok':
      return null;
    case 'invalid':
      return messages.invalidProject;
    case 'notFound':
      return messages.projectNotFound;
    case 'forbidden':
      return messages.notProjectOwner;
    case 'completed':
      return messages.projectCompleted;
    default:
      return messages.somethingFailed;
  }
}
