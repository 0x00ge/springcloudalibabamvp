declare module 'mockjs' {
  interface Mockjs {
    Random: {
      email(): string
      guid(): string
      integer(min: number, max: number): number
      pick<T>(list: T[]): T
    }
    mock<T = unknown>(template: unknown): T
    mock<T = unknown>(url: string | RegExp, type: string, template: unknown): T
    setup(settings: { timeout?: string | number }): void
  }

  const Mock: Mockjs

  export default Mock
}
