import { describe, expect, it } from 'vitest';
import { messages } from './messages';
import { checkFields, checkText, fieldMessagesFrom, hasProblems, refusal, RULES, textLength } from './validation';

describe('textLength', () => {
  it('counts Unicode characters of the trimmed text, as the API does', () => {
    expect(textLength('  abc  ')).toBe(3);
    expect(textLength('😀😀')).toBe(2);
    expect(textLength('\u00a0\u00a0')).toBe(0);
  });

  it('uses Java whitespace plus space separators, without removing FEFF or zero-width space', () => {
    expect(textLength('\u001c\u001d\u001e\u001f\u00a0\u2007\u202fabc\u3000')).toBe(3);
    expect(textLength('\ufeff')).toBe(1);
    expect(textLength('\u200b')).toBe(1);
    expect(checkText('\u001cx\u001f', { maximum: 1, tooLong: 'too long' })).toBeUndefined();
    expect(checkText('\ufeffx', { maximum: 1, tooLong: 'too long' })).toBe('too long');
  });
});

describe('checkText', () => {
  it('refuses a blank required text, and accepts a blank optional one', () => {
    expect(checkText('   ', RULES.projectName)).toBe(messages.nameRequired);
    expect(checkText('', RULES.projectDescription)).toBeUndefined();
  });

  it('accepts the maximum and refuses one character more', () => {
    expect(checkText('x'.repeat(100), RULES.projectName)).toBeUndefined();
    expect(checkText('x'.repeat(101), RULES.projectName)).toBe(messages.nameTooLong);
    expect(checkText('😀'.repeat(150), RULES.taskTitle)).toBeUndefined();
    expect(checkText('😀'.repeat(151), RULES.taskTitle)).toBe(messages.titleTooLong);
    expect(checkText('x'.repeat(2001), RULES.comment)).toBe(messages.commentTooLong);
  });
});

describe('checkFields', () => {
  it('names each field that breaks its rule', () => {
    const rules = { name: RULES.projectName, description: RULES.projectDescription };
    const found = checkFields({ name: '', description: 'x'.repeat(1001) }, rules);
    expect(found).toEqual({ name: messages.nameRequired, description: messages.descriptionTooLong });
    expect(hasProblems(found)).toBe(true);
    expect(hasProblems(checkFields({ name: 'Mobile App', description: '' }, rules))).toBe(false);
  });
});

describe('fieldMessagesFrom', () => {
  it('turns the API problems into the User interface messages, by pointer', () => {
    const rules = { title: RULES.taskTitle, description: RULES.taskDescription };
    expect(
      fieldMessagesFrom(
        [
          { pointer: '/title', detail: 'title is required' },
          { pointer: '/description', detail: 'too long' },
          { pointer: '/other', detail: 'ignored' },
        ],
        rules,
      ),
    ).toEqual({ title: messages.titleRequired, description: messages.taskDescriptionTooLong });
  });
});

describe('refusal', () => {
  it('names each refusal for its subject', () => {
    expect(refusal({ outcome: 'notFound' }, 'project')).toBe(messages.projectNotFound);
    expect(refusal({ outcome: 'notFound' }, 'task')).toBe(messages.taskNotFound);
    expect(refusal({ outcome: 'invalid', problems: [] }, 'comment')).toBe(messages.invalidComment);
    expect(refusal({ outcome: 'forbidden' }, 'task')).toBe(messages.notProjectOwner);
    expect(refusal({ outcome: 'completed' }, 'task')).toBe(messages.projectCompleted);
    expect(refusal({ outcome: 'failed' }, 'project')).toBe(messages.somethingFailed);
  });

  it('uses the text given for a refusal instead', () => {
    expect(refusal({ outcome: 'forbidden' }, 'task', { forbidden: messages.stateForbidden })).toBe(messages.stateForbidden);
  });
});
