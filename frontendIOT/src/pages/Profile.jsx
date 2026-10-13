import { useEffect, useState } from 'react'
import { getProfile } from '../api'

const docs = [
  { icon: '📄', title: 'System Documentation', desc: 'Complete guide for Smart Home IoT system', tag: 'PDF', cls: 'blue' },
  { icon: '</>', title: 'PostMan', desc: 'REST API endpoints and integration guide', tag: 'API', cls: 'green', href: 'https://documenter.getpostman.com/view/47383257/2sBYHRV1jG' },
  { icon: '⑂', title: 'GitHub Repository', desc: 'Source code and project files', tag: 'CODE', cls: 'green' },
  { icon: '✎', title: 'Figma Design', desc: 'UI/UX design files and components', tag: 'DESIGN', cls: 'red' },
]

export default function Profile() {
  const [profile, setProfile] = useState({ name: 'Hoàng Xuân Vinh', description: 'IoT Developer' })
  useEffect(() => { getProfile().then((p) => p && setProfile(p)).catch(() => {}) }, [])

  return (
    <>
      <section className="profile">
        <div className="avatar"><img src="/avatar.jpeg" alt={profile.name} /></div>
        <div>
          <h2>{profile.name}</h2>
          <p>{profile.description}</p>
          <b>PTIT</b>
          <hr />
          <small>✉ hhinhxuanvang@gmail.com &nbsp;&nbsp; 📍 Building PTIT Ngọc Trục, Room 401</small>
        </div>
      </section>

      <h3>Documentation &amp; Resources</h3>
      <div className="docs">
        {docs.map((d) => (
          <a key={d.title} href={d.href || '#'} {...(d.href && { target: '_blank', rel: 'noreferrer' })} className="doc">
            <span className="icon">{d.icon}</span>
            <div><b>{d.title}</b><small>{d.desc}</small></div>
            <span className={'tag ' + d.cls}>{d.tag}</span>
          </a>
        ))}
      </div>
      <p className="footer">© 2026 Smart Home</p>
    </>
  )
}
