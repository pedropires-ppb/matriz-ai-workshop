import { describe, expect, it } from 'vitest';
import { ALL_PROJECTS, FIRST_PAGE, formatRoute, HOME, NEW_PROJECT, parseRoute, type Route } from './router';

describe('parseRoute', () => {
  it('reads Home from an empty address or from #/', () => {
    expect(parseRoute('')).toEqual(HOME);
    expect(parseRoute('#')).toEqual(HOME);
    expect(parseRoute('#/')).toEqual(HOME);
  });

  it('reads Projects with its search and page', () => {
    expect(parseRoute('#/projects')).toEqual(ALL_PROJECTS);
    expect(parseRoute('#/projects?name=%20Mobile%20&page=3')).toEqual({ ...ALL_PROJECTS, name: 'Mobile', page: 3 });
    expect(parseRoute('#/projects/new')).toEqual(NEW_PROJECT);
  });

  it('reads a project with its filters and page, in their canonical order, once each', () => {
    expect(parseRoute('#/projects/p-1?state=DONE&state=TO_DO&state=DONE&priority=HIGH&page=2')).toEqual({
      screen: 'project',
      projectId: 'p-1',
      list: { states: ['TO_DO', 'DONE'], priorities: ['HIGH'], page: 2 },
    });
  });

  it('reads a task with the list it was opened from', () => {
    expect(parseRoute('#/projects/p-1/tasks/t-9?priority=LOW')).toEqual({
      screen: 'task',
      projectId: 'p-1',
      taskId: 't-9',
      list: { states: [], priorities: ['LOW'], page: 1 },
    });
  });

  it('ignores unknown filter values and pages that are not whole numbers of at least 1', () => {
    for (const page of ['0', '-1', '1.5', 'abc', '', '01', '99999999999']) {
      expect(parseRoute(`#/projects/p?page=${page}&state=LATER`)).toEqual({ screen: 'project', projectId: 'p', list: FIRST_PAGE });
    }
  });

  it('names no screen for other addresses', () => {
    for (const hash of ['#/elsewhere', '#/projects/p/tasks', '#/projects/p/other/t', '#/projects/p/tasks/t/more', '#/projects/%E0%A4%A']) {
      expect(parseRoute(hash)).toEqual({ screen: 'unknown' });
    }
  });
});

describe('formatRoute', () => {
  const routes: Route[] = [
    HOME,
    ALL_PROJECTS,
    NEW_PROJECT,
    { ...ALL_PROJECTS, name: 'A & B / C', page: 2 },
    { screen: 'project', projectId: 'p 1/2', list: FIRST_PAGE },
    { screen: 'project', projectId: 'p', list: { states: ['IN_PROGRESS'], priorities: ['LOW', 'HIGH'], page: 4 } },
    { screen: 'task', projectId: 'p', taskId: 't?#', list: { states: ['DONE'], priorities: [], page: 1 } },
  ];

  it('gives an address that names the same screen', () => {
    for (const route of routes) {
      expect(parseRoute(formatRoute(route))).toEqual(route);
    }
  });

  it('leaves out the first page and empty filters', () => {
    expect(formatRoute({ screen: 'project', projectId: 'p', list: FIRST_PAGE })).toBe('#/projects/p');
    expect(formatRoute(ALL_PROJECTS)).toBe('#/projects');
    expect(formatRoute({ screen: 'unknown' })).toBe('#/');
  });
});
