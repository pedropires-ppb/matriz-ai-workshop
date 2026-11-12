/**
 * Lets one piece of work run at a time: while it runs, another request to run is ignored, so a repeated
 * click or key press sends one request (I13 scope decision 9). It holds no state of the screen.
 */
export class Gate {
  private running = false;

  get busy(): boolean {
    return this.running;
  }

  /** Runs `work`, or does nothing and returns `undefined` if earlier work has not finished. */
  async run<R>(work: () => Promise<R>): Promise<R | undefined> {
    if (this.running) {
      return undefined;
    }
    this.running = true;
    try {
      return await work();
    } finally {
      this.running = false;
    }
  }
}
