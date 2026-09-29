import { useState } from 'react'
import './App.css'
import { UploadForm } from './components/UploadForm'
import { JobProgress } from './components/JobProgress'
import { ScoringConfigForm } from './components/ScoringConfigForm'
import { Dashboard } from './components/Dashboard'
import type { JobResponse, ScoringSummaryResponse } from './api/types'

function App() {
  const [job, setJob] = useState<JobResponse | null>(null)
  const [scoringSummary, setScoringSummary] = useState<ScoringSummaryResponse | null>(null)

  function handleUploaded(newJob: JobResponse) {
    setJob(newJob)
    setScoringSummary(null)
  }

  function handleJobUpdate(updated: JobResponse) {
    setJob(updated)
  }

  function handleReset() {
    setJob(null)
    setScoringSummary(null)
  }

  return (
    <div className="app">
      <header className="app-header">
        <h1>TierForge</h1>
        <p>Resilient bulk store scoring &amp; tiering</p>
      </header>

      {!job && <UploadForm onUploaded={handleUploaded} />}

      {job && (
        <>
          <button type="button" className="reset-link" onClick={handleReset}>
            ← Start a new job
          </button>
          <JobProgress job={job} onJobUpdate={handleJobUpdate} />
        </>
      )}

      {job && job.status === 'COMPLETED' && (
        <ScoringConfigForm jobId={job.id} onScored={setScoringSummary} />
      )}

      {job && scoringSummary && <Dashboard jobId={job.id} summary={scoringSummary} />}
    </div>
  )
}

export default App
