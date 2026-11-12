// The rules of the texts that people type, checked before a request so the person sees them at once,
// and the messages for what the API refuses. The API checks the same rules again.
import type { FieldProblem, Refused } from './http';
import { messages } from './messages';

/** Java whitespace plus Unicode space separators (A-033); FEFF and zero-width space are preserved. */
export function trimText(text: string): string {
  return text.replace(/^[\p{Zs}\u0009-\u000d\u001c-\u0020\u2028\u2029]+|[\p{Zs}\u0009-\u000d\u001c-\u0020\u2028\u2029]+$/gu, '');
}

/** Counts Unicode code points after the API's trimming, not UTF-16 units (A-031/A-033/A-036). */
export function textLength(text: string): number {
  return Array.from(trimText(text)).length;
}

export interface TextRule {
  maximum: number;
  /** The message for an empty text; absent if the text is optional. */
  required?: string;
  tooLong: string;
}

export const RULES = {
  projectName: { maximum: 100, required: messages.nameRequired, tooLong: messages.nameTooLong },
  projectDescription: { maximum: 1000, tooLong: messages.descriptionTooLong },
  taskTitle: { maximum: 150, required: messages.titleRequired, tooLong: messages.titleTooLong },
  taskDescription: { maximum: 2000, tooLong: messages.taskDescriptionTooLong },
  comment: { maximum: 2000, required: messages.commentRequired, tooLong: messages.commentTooLong },
} satisfies Record<string, TextRule>;

/** The message for a text that breaks its rule, or `undefined`. */
export function checkText(text: string, rule: TextRule): string | undefined {
  const length = textLength(text);
  if (length === 0 && rule.required !== undefined) {
    return rule.required;
  }
  return length > rule.maximum ? rule.tooLong : undefined;
}

export type FieldMessages<K extends string> = Partial<Record<K, string>>;

/** The message for each field that breaks its rule. */
export function checkFields<K extends string>(values: Record<K, string>, rules: Record<K, TextRule>): FieldMessages<K> {
  const found: FieldMessages<K> = {};
  for (const field of Object.keys(rules) as K[]) {
    const problem = checkText(values[field], rules[field]);
    if (problem !== undefined) {
      found[field] = problem;
    }
  }
  return found;
}

export function hasProblems<K extends string>(found: FieldMessages<K>): boolean {
  return Object.values(found).some((problem) => problem !== undefined);
}

/** The messages for the members that the API refused, by their JSON Pointer, in the User interface's words. */
export function fieldMessagesFrom<K extends string>(problems: FieldProblem[], rules: Record<K, TextRule>): FieldMessages<K> {
  const found: FieldMessages<K> = {};
  for (const problem of problems) {
    const field = problem.pointer.replace(/^\//, '') as K;
    const rule = rules[field];
    if (rule !== undefined) {
      found[field] = rule.required !== undefined && problem.detail.includes('required') ? rule.required : rule.tooLong;
    }
  }
  return found;
}

export type Subject = 'project' | 'task' | 'comment';

const INVALID: Record<Subject, string> = {
  project: messages.invalidProject,
  task: messages.invalidTask,
  comment: messages.invalidComment,
};

const NOT_FOUND: Record<Subject, string> = {
  project: messages.projectNotFound,
  task: messages.taskNotFound,
  comment: messages.taskNotFound,
};

/** The text for a refusal, about a project, a task, or a comment, unless `texts` gives another one. */
export function refusal(
  result: Refused,
  subject: Subject,
  texts: Partial<Record<Refused['outcome'], string>> = {},
): string {
  const given = texts[result.outcome];
  if (given !== undefined) {
    return given;
  }
  switch (result.outcome) {
    case 'invalid':
      return INVALID[subject];
    case 'notFound':
      return NOT_FOUND[subject];
    case 'forbidden':
      return messages.notProjectOwner;
    case 'completed':
      return messages.projectCompleted;
    default:
      return messages.somethingFailed;
  }
}
