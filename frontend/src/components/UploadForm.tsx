import { useState } from 'react'
import type { FormEvent } from 'react'
import { ApiError, uploadJob } from '../api/client'
import type { JobResponse } from '../api/types'

interface Props {
  onUploaded: (job: JobResponse) => void
}

export function UploadForm({ onUploaded }: Props) {
  const [file, setFile] = useState<File | null>(null)
  const [errors, setErrors] = useState<string[]>([])
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!file) {
      return
    }
    setSubmitting(true)
    setErrors([])
    try {
      const job = await uploadJob(file)
      onUploaded(job)
    } catch (err) {
      setErrors(err instanceof ApiError ? err.messages : ['Upload failed. Is the backend running?'])
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section className="card">
      <h2>1. Upload store list</h2>
      <p className="hint">
        CSV with columns: store_id, store_name, address, city, state, country.
      </p>
      <form onSubmit={handleSubmit}>
        <input
          type="file"
          accept=".csv"
          onChange={(e) => setFile(e.target.files?.[0] ?? null)}
        />
        <button type="submit" disabled={!file || submitting}>
          {submitting ? 'Uploading…' : 'Upload'}
        </button>
      </form>
      {errors.length > 0 && (
        <ul className="error-list">
          {errors.map((message) => (
            <li key={message}>{message}</li>
          ))}
        </ul>
      )}
    </section>
  )
}
